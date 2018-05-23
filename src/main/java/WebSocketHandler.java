import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

//SimpleChannelInboundHandler, which allows to explicit only handle a specific type of messages
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {
		
		if(obj instanceof FullHttpRequest){
			//检查是否为websocket握手信息
			FullHttpRequest request = (FullHttpRequest)obj;
			if(request.headers().get(HttpHeaderNames.UPGRADE).equals("websocket")){
				WebSocketServerHandshakerFactory wf = new WebSocketServerHandshakerFactory("ws://" + request.headers().get(HttpHeaderNames.HOST) + "/ws", null, false);
				WebSocketServerHandshaker handshaker = wf.newHandshaker(request);
				if(handshaker == null){
					//握手失败，可能原因：版本不支持
					sendSimpleResponse(ctx, HttpResponseStatus.BAD_REQUEST);
					ctx.close();
					return;
				}
				handshaker.handshake(ctx.channel(), request);
				
			}else{
				sendSimpleResponse(ctx, HttpResponseStatus.NOT_FOUND);
				ctx.close();
				return;
			}
		}else if(obj instanceof WebSocketFrame){
			if(obj instanceof CloseWebSocketFrame){
				CloseWebSocketFrame frame = new CloseWebSocketFrame();
				ctx.writeAndFlush(frame);
				ctx.close();
				System.out.println("closed");
			}else if(obj instanceof BinaryWebSocketFrame){
				TextWebSocketFrame frame = new TextWebSocketFrame();
				ByteBuf buffer = Unpooled.copiedBuffer("only support text",CharsetUtil.UTF_8);
				frame.content().writeBytes(buffer);
				ctx.writeAndFlush(frame);
				buffer.release();
			}else if(obj instanceof TextWebSocketFrame){
				String text = ((TextWebSocketFrame)obj).text();
				TextWebSocketFrame frame = new TextWebSocketFrame();
				ByteBuf buffer = Unpooled.copiedBuffer("got it:" + text,CharsetUtil.UTF_8);
				frame.content().writeBytes(buffer);
				ctx.writeAndFlush(frame);
				buffer.release();
			}
		}
		
	}
	
	public void sendSimpleResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH,0);
		ctx.writeAndFlush(response);
	}
}
