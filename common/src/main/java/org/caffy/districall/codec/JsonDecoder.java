package org.caffy.districall.codec;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.caffy.districall.beans.EmptyResponse;
import org.caffy.districall.beans.ExchangeFrame;
import org.caffy.districall.beans.RemoteMethod;
import org.caffy.districall.beans.RemoteMethodResponse;
import org.caffy.districall.exception.ProtocolException;
import org.caffy.districall.transfer.RequestMapper;
import org.caffy.districall.utils.ImplementationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

/**
 * 基于 Json 的解码器
 */
@SuppressWarnings("unused")
public class JsonDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(JsonDecoder.class);
    private static Charset utf8 = Charset.forName("utf-8");

    private Gson gson = new Gson();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() == 0) return;

        try {
            ByteBufInputStream inputStream = new ByteBufInputStream(in);

            JsonParser parser = new JsonParser();
            InputStreamReader streamReader = new InputStreamReader(inputStream, utf8);
            JsonReader reader = new JsonReader(streamReader);
            reader.setLenient(true);
            JsonElement element = parser.parse(reader);
            JsonObject root = element.getAsJsonObject();

            long id = root.get("id").getAsLong();
            JsonElement typeElement = root.get("type");
            String type = typeElement == null ? null : typeElement.getAsString();

            Object object;
            if (type == null) {
                RequestMapper.Returned returned = RequestMapper.pop(id);
                if (returned == null)
                    throw new ProtocolException();
                object = new EmptyResponse(returned.callback);
            } else if ("RemoteMethod".equals(type)) {
                JsonElement v = root.get("v");
                object = decodeMethod(id, v.getAsJsonObject());
            } else if ("RemoteMethodResponse".equals(type)) {
                RequestMapper.Returned returned = RequestMapper.pop(id);
                if (returned == null)
                    throw new ProtocolException();
                RemoteMethodResponse response = new RemoteMethodResponse();
                response.setType(returned.type);
                response.setCallback(returned.callback);

                JsonElement o = root.get("o");
                if (o != null) {
                    Object obj = gson.fromJson(o, returned.type);
                    response.setObject(obj);
                }
                object = response;
            } else {
                ClassLoader loader = JsonDecoder.class.getClassLoader();
                Class<?> c = loader.loadClass("org.caffy.districall.beans." + type);
                JsonElement v = root.get("v");
                object = gson.fromJson(v.getAsJsonObject(), c);
            }
            out.add(new ExchangeFrame(id, object));
        } catch (Exception e) {
            logger.error("Decode exception", e);
        }
    }

    private RemoteMethod decodeMethod(long id, JsonObject element) throws Exception {
        RemoteMethod method = new RemoteMethod();

        String interfaceName = element.get("interface").getAsString();
        String methodName = element.get("method").getAsString();
        JsonElement session = element.get("session");
        if (session != null) {
            String u = session.getAsString();
            method.setSession(UUID.fromString(u));
        }

        Method m = ImplementationFactory.queryMethodByName(interfaceName, methodName);
        if (m == null)
            throw new NoSuchMethodException();
        method.setMethod(m);

        JsonElement parameters = element.get("parameters");
        if (parameters != null) {
            JsonArray array = parameters.getAsJsonArray();
            Class<?>[] types = m.getParameterTypes();
            if (types.length > 0) {
                Object[] objects = new Object[types.length];

                for (int i = 0; i < types.length; i++) {
                    Class<?> type = types[i];
                    JsonElement jsonElement = array.get(i);
                    if (type.isInterface() || type.isAssignableFrom(UUID.class)) {
                        String s = jsonElement.getAsString();
                        objects[i] = UUID.fromString(s);
                    } else {
                        Object o = gson.fromJson(jsonElement, type);
                        objects[i] = o;
                    }
                }
                method.setParameters(objects);
            }
        }
        return method;
    }
}
