/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.commit451.zapdos;

import com.commit451.zapdos.drive.Body;
import com.commit451.zapdos.drive.CREATE;
import com.commit451.zapdos.drive.DELETE;
import com.commit451.zapdos.drive.Path;
import com.commit451.zapdos.drive.READ;
import com.commit451.zapdos.drive.UPDATE;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.MetadataBuffer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapts an invocation of an interface method into an HTTP call.
 */
final class ServiceMethod<T> {
    // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
    static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);

    private final Scope baseScope;
    private final Converter<MetadataBuffer, T> responseConverter;
    private final String httpMethod;
    private final String relativeUrl;
    private final boolean hasBody;
    private final ParameterHandler<?>[] parameterHandlers;

    ServiceMethod(Builder<T> builder) {
        this.baseScope = builder.zapdos.scope;
        this.responseConverter = builder.responseConverter;
        this.httpMethod = builder.httpMethod;
        this.relativeUrl = builder.relativeUrl;
        this.hasBody = builder.hasBody;
        this.parameterHandlers = builder.parameterHandlers;
    }

    /**
     * Builds an HTTP request from method arguments.
     */
    Request toRequest(Object... args) throws IOException {
        RequestBuilder requestBuilder = new RequestBuilder(httpMethod, baseScope, relativeUrl);

        @SuppressWarnings("unchecked") // It is an error to invoke a method with the wrong arg types.
                ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

        int argumentCount = args != null ? args.length : 0;
        if (argumentCount != handlers.length) {
            throw new IllegalArgumentException("Argument count (" + argumentCount
                    + ") doesn't match expected count (" + handlers.length + ")");
        }

        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(requestBuilder, args[p]);
        }

        return requestBuilder.build();
    }

    /**
     * Builds a method return value from an HTTP response body.
     */
    T toResponse(MetadataBuffer body) throws IOException {
        return responseConverter.convert(body);
    }

    public boolean hasBody() {
        return hasBody;
    }

    /**
     * Inspects the annotations on an interface method to construct a reusable service method. This
     * requires potentially-expensive reflection so it is best to build each service method only once
     * and reuse it. Builders cannot be reused.
     */
    static final class Builder<T> {
        final Zapdos zapdos;
        final Method method;
        final Annotation[] methodAnnotations;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;

        Type responseType;
        boolean gotField;
        boolean gotPart;
        boolean gotBody;
        boolean gotPath;
        boolean gotQuery;
        boolean gotUrl;
        String httpMethod;
        boolean hasBody;
        boolean isFormEncoded;
        boolean isMultipart;
        String relativeUrl;
        Set<String> relativeUrlParamNames;
        ParameterHandler<?>[] parameterHandlers;
        Converter<MetadataBuffer, T> responseConverter;

        public Builder(Zapdos zapdos, Method method) {
            this.zapdos = zapdos;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
            this.parameterAnnotationsArray = method.getParameterAnnotations();
        }

        public ServiceMethod build() {
            //Hard coded this, different in that way from Retrofit
            responseType = Utils.getTypeFromMethod(method);
            responseConverter = createResponseConverter();

            for (Annotation annotation : methodAnnotations) {
                parseMethodAnnotation(annotation);
            }

            if (httpMethod == null) {
                throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
            }

            if (!hasBody) {
                if (isMultipart) {
                    throw methodError(
                            "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
                }
                if (isFormEncoded) {
                    throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
                            + "request body (e.g., @POST).");
                }
            }

            int parameterCount = parameterAnnotationsArray.length;
            parameterHandlers = new ParameterHandler<?>[parameterCount];
            for (int p = 0; p < parameterCount; p++) {
                Type parameterType = parameterTypes[p];
                if (Utils.hasUnresolvableType(parameterType)) {
                    throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
                            parameterType);
                }

                Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
                if (parameterAnnotations == null) {
                    throw parameterError(p, "No Retrofit annotation found.");
                }

                parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
            }

            if (relativeUrl == null && !gotUrl) {
                throw methodError("Missing either @%s URL or @Url parameter.", httpMethod);
            }
            if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
                throw methodError("Non-body HTTP method cannot contain @Body.");
            }
            if (isFormEncoded && !gotField) {
                throw methodError("Form-encoded method must contain at least one @Field.");
            }
            if (isMultipart && !gotPart) {
                throw methodError("Multipart method must contain at least one @Part.");
            }

            return new ServiceMethod<>(this);
        }

        private void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof CREATE) {
                parseHttpMethodAndPath("CREATE", ((CREATE) annotation).value(), true);
            } else if (annotation instanceof READ) {
                parseHttpMethodAndPath("READ", ((READ) annotation).value(), false);
            } else if (annotation instanceof UPDATE) {
                parseHttpMethodAndPath("UPDATE", ((UPDATE) annotation).value(), true);
            } else if (annotation instanceof DELETE) {
                parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), true);
            }
        }

        private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
            if (this.httpMethod != null) {
                throw methodError("Only one HTTP method is allowed. Found: %s and %s.",
                        this.httpMethod, httpMethod);
            }
            this.httpMethod = httpMethod;
            this.hasBody = hasBody;

            if (value.isEmpty()) {
                return;
            }

            // Get the relative URL path and existing query string, if present.
            int question = value.indexOf('?');
            if (question != -1 && question < value.length() - 1) {
                // Ensure the query string does not have any named parameters.
                String queryParams = value.substring(question + 1);
                Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
                if (queryParamMatcher.find()) {
                    throw methodError("URL query string \"%s\" must not have replace block. "
                            + "For dynamic query parameters use @Query.", queryParams);
                }
            }

            this.relativeUrl = value;
            this.relativeUrlParamNames = parsePathParameters(value);
        }

        private ParameterHandler<?> parseParameter(
                int p, Type parameterType, Annotation[] annotations) {
            ParameterHandler<?> result = null;
            for (Annotation annotation : annotations) {
                ParameterHandler<?> annotationAction = parseParameterAnnotation(
                        p, parameterType, annotations, annotation);

                if (annotationAction == null) {
                    continue;
                }

                if (result != null) {
                    throw parameterError(p, "Multiple Retrofit annotations found, only one allowed.");
                }

                result = annotationAction;
            }

            if (result == null) {
                throw parameterError(p, "No Retrofit annotation found.");
            }

            return result;
        }

        private ParameterHandler<?> parseParameterAnnotation(
                int p, Type type, Annotation[] annotations, Annotation annotation) {
            if (annotation instanceof Path) {
                if (gotQuery) {
                    throw parameterError(p, "A @Path parameter must not come after a @Query.");
                }
                if (gotUrl) {
                    throw parameterError(p, "@Path parameters may not be used with @Url.");
                }
                if (relativeUrl == null) {
                    throw parameterError(p, "@Path can only be used with relative url on @%s", httpMethod);
                }
                gotPath = true;

                Path path = (Path) annotation;
                String name = path.value();
                validatePathName(p, name);

                Converter<?, String> converter = ToStringConverter.INSTANCE;
                return new ParameterHandler.Path<>(name, converter, path.encoded());

            } else if (annotation instanceof Body) {
                if (isFormEncoded || isMultipart) {
                    throw parameterError(p,
                            "@Body parameters cannot be used with form or multi-part encoding.");
                }
                if (gotBody) {
                    throw parameterError(p, "Multiple @Body method annotations found.");
                }

                Converter<?, RequestBody> converter;
                try {
                    converter = zapdos.requestBodyConverter(type, annotations, methodAnnotations);
                } catch (RuntimeException e) {
                    // Wide exception range because factories are user code.
                    throw parameterError(e, p, "Unable to create @Body converter for %s", type);
                }
                gotBody = true;
                return new ParameterHandler.Body<>(converter);
            }

            return null; // Not a Retrofit annotation.
        }

        private void validatePathName(int p, String name) {
            if (!PARAM_NAME_REGEX.matcher(name).matches()) {
                throw parameterError(p, "@Path parameter name must match %s. Found: %s",
                        PARAM_URL_REGEX.pattern(), name);
            }
            // Verify URL replacement name is actually present in the URL path.
            if (!relativeUrlParamNames.contains(name)) {
                throw parameterError(p, "URL \"%s\" does not contain \"{%s}\".", relativeUrl, name);
            }
        }

        private Converter<MetadataBuffer, T> createResponseConverter() {
            Annotation[] annotations = method.getAnnotations();
            try {
                return zapdos.responseBodyConverter(responseType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create converter for %s", responseType);
            }
        }

        private RuntimeException methodError(String message, Object... args) {
            return methodError(null, message, args);
        }

        private RuntimeException methodError(Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for method "
                    + method.getDeclaringClass().getSimpleName()
                    + "."
                    + method.getName(), cause);
        }

        private RuntimeException parameterError(
                Throwable cause, int p, String message, Object... args) {
            return methodError(cause, message + " (parameter #" + (p + 1) + ")", args);
        }

        private RuntimeException parameterError(int p, String message, Object... args) {
            return methodError(message + " (parameter #" + (p + 1) + ")", args);
        }
    }

    /**
     * Gets the set of unique path parameters used in the given URI. If a parameter is used twice
     * in the URI, it will only show up once in the set.
     */
    static Set<String> parsePathParameters(String path) {
        Matcher m = PARAM_URL_REGEX.matcher(path);
        Set<String> patterns = new LinkedHashSet<>();
        while (m.find()) {
            patterns.add(m.group(1));
        }
        return patterns;
    }

    static Class<?> boxIfPrimitive(Class<?> type) {
        if (boolean.class == type) return Boolean.class;
        if (byte.class == type) return Byte.class;
        if (char.class == type) return Character.class;
        if (double.class == type) return Double.class;
        if (float.class == type) return Float.class;
        if (int.class == type) return Integer.class;
        if (long.class == type) return Long.class;
        if (short.class == type) return Short.class;
        return type;
    }
}
