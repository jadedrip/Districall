package org.caffy.districall.codec;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.caffy.districall.utils.ImplementationFactory;
import org.caffy.districall.beans.ExchangeFrame;
import org.caffy.districall.beans.RemoteMethod;
import org.caffy.districall.beans.RemoteMethodResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * 使用 Json 的方式来编码
 */
@SuppressWarnings("unused")
public class JsonEncoder extends MessageToByteEncoder<ExchangeFrame> {
    private static final Logger logger = LoggerFactory.getLogger(JsonEncoder.class);
    private static Charset utf8 = Charset.forName("utf-8");
    private Gson gson = new Gson();

    private void writeParameters(JsonWriter writer, Object[] parameters) throws IOException {
        if (parameters == null || parameters.length == 0) return;
        writer.name("parameters");
        writer.beginArray();
        for (Object i : parameters) {
            gson.toJson(i, i.getClass(), writer);
        }
        writer.endArray();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ExchangeFrame frame, ByteBuf out) throws Exception {
        ByteBufOutputStream stream = new ByteBufOutputStream(out);
        OutputStreamWriter streamWriter = new OutputStreamWriter(stream, utf8);
        JsonWriter writer = new JsonWriter(streamWriter);
        writer.beginObject();

        Object msg = frame.getData();
        assert msg != null;
        writer.name("id").value(frame.getSerial());
        writer.name("type").value(msg.getClass().getSimpleName());
        if (msg instanceof RemoteMethod) {
            encodeMethod(writer, (RemoteMethod) msg);
        } else if (msg instanceof RemoteMethodResponse) {
            RemoteMethodResponse v = (RemoteMethodResponse) msg;
            Object object = v.getObject();
            if (object != null) {
                writer.name("o");
                gson.toJson(object, object.getClass(), writer);
            }
        } else {
            writer.name("v");
            gson.toJson(msg, msg.getClass(), writer);
        }

        writer.endObject();
        writer.close();
//        streamWriter.close();
//        stream.close();
    }

    private void encodeMethod(JsonWriter writer, RemoteMethod msg) throws IOException {
        writer.name("v");
        writer.beginObject();
        UUID session = msg.getSession();
        if (session != null) {
            writer.name("session").value(session.toString());
        }
        Method method = msg.getMethod();
        String interfaceName = method.getDeclaringClass().getName();
        String methodName = ImplementationFactory.getMethodName(method);
        writer.name("interface").value(interfaceName);
        writer.name("method").value(methodName);
        writeParameters(writer, msg.getParameters());
        writer.endObject();
    }
}
