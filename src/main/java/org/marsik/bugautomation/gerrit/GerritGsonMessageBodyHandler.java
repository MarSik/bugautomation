package org.marsik.bugautomation.gerrit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ApplicationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class GerritGsonMessageBodyHandler implements MessageBodyWriter<Object>,
        MessageBodyReader<Object> {

    private static final String UTF_8 = "UTF-8";
    private static final String OLDDATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS";
    private static final String TIMESTAMP_FORMAT = "uuuu-MM-dd HH:mm:ss.SSSSSSSSS";

    private Gson gson;

    private Gson getGson() {
        if (gson == null) {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            gson = gsonBuilder
                    .setLenient()
                    .setDateFormat(OLDDATE_FORMAT)
                    .registerTypeAdapter(Instant.class, new InstantConverter(TIMESTAMP_FORMAT))
                    .create();
        }
        return gson;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            java.lang.annotation.Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws ApplicationException, IOException {
        InputStreamReader streamReader = new InputStreamReader(entityStream, UTF_8);
        try {
            Type jsonType;
            if (type.equals(genericType)) {
                jsonType = type;
            } else {
                jsonType = genericType;
            }
            return getGson().fromJson(streamReader, jsonType);
        } finally {
            streamReader.close();
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        OutputStreamWriter writer = new OutputStreamWriter(entityStream, UTF_8);
        try {
            Type jsonType;
            if (type.equals(genericType)) {
                jsonType = type;
            } else {
                jsonType = genericType;
            }
            getGson().toJson(object, jsonType, writer);
        } finally {
            writer.close();
        }
    }

    public static class InstantConverter implements JsonSerializer<Instant>, JsonDeserializer<Instant>
    {
        private final DateTimeFormatter timeFormatter;

        public InstantConverter(String timeFormat) {
            this.timeFormatter = DateTimeFormatter.ofPattern(timeFormat);
        }

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(timeFormatter.format(src));
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException
        {
            return LocalDateTime.parse(json.getAsString(), timeFormatter).toInstant(ZoneOffset.UTC);
        }
    }
}
