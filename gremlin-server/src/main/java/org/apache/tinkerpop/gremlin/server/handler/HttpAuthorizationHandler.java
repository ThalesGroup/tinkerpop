/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.util.Attribute;
import io.netty.util.AttributeMap;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.server.auth.AuthenticatedUser;
import org.apache.tinkerpop.gremlin.server.auth.AuthenticationException;
import org.apache.tinkerpop.gremlin.server.auth.Authenticator;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationException;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationRequest;
import org.apache.tinkerpop.gremlin.server.authorization.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.apache.tinkerpop.gremlin.groovy.jsr223.dsl.credential.CredentialGraphTokens.PROPERTY_PASSWORD;
import static org.apache.tinkerpop.gremlin.groovy.jsr223.dsl.credential.CredentialGraphTokens.PROPERTY_USERNAME;

/**
 * Implements authorization for use with the {@link HttpGremlinEndpointHandler} and HTTP based API calls.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HttpAuthorizationHandler extends AbstractAuthorizationHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpAuthorizationHandler.class);
    private static final Logger auditLogger = LoggerFactory.getLogger(GremlinServer.AUDIT_LOGGER_NAME);
    private final Settings.AuthorizationSettings authorizationSettings;

    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    public HttpAuthorizationHandler(final Authorizer authorizer,
                                    final Settings.AuthorizationSettings authorizationSettings) {
        super(authorizer);
        this.authorizationSettings = authorizationSettings;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        logger.info("inside HttpAuthorizationHandler.channelRead");
        if (msg instanceof FullHttpMessage) {
            final FullHttpMessage request = (FullHttpMessage) msg;
//            if (!request.headers().contains("Authorization")) {
//                sendError(ctx, msg);
//                return;
//            }

            ByteBuf jsonBuf = request.content();
            String jsonStr = jsonBuf.toString(CharsetUtil.UTF_8);
            logger.info("request body:: " + jsonStr);

            final Attribute<String> user = ((AttributeMap) ctx).attr(StateKey.AUTHENTICATED_USER);
            logger.info("user:: " + user);

            // strip off "Basic " from the Authorization header (RFC 2617)
//            final String basic = "Basic ";
//            final String authorizationHeader = request.headers().get("Authorization");
//            if (!authorizationHeader.startsWith(basic)) {
//                sendError(ctx, msg);
//                return;
//            }
//            byte[] decodedUserPass = null;
//            try {
//                final String encodedUserPass = authorizationHeader.substring(basic.length());
//                decodedUserPass = decoder.decode(encodedUserPass);
//            } catch (IndexOutOfBoundsException iae) {
//                sendError(ctx, msg);
//                return;
//            } catch (IllegalArgumentException iae) {
//                sendError(ctx, msg);
//                return;
//            }
//            final String authorization = new String(decodedUserPass, Charset.forName("UTF-8"));
//            final String[] split = authorization.split(":");
//            if (split.length != 2) {
//                sendError(ctx, msg);
//                return;
//            }

//            String userName = split[0];
            //final Map<String,String> credentials = new HashMap<>();
//            credentials.put(PROPERTY_USERNAME, split[0]);
//            credentials.put(PROPERTY_PASSWORD, split[1]);

            try {
                logger.debug("Authorizing for user : " + user.get());
                boolean hasWriteStep = false;
                String resource = "graph";
                authorize(user.get(), hasWriteStep, resource, ctx);

//                if(!accessAllowed){
//                    throw new AuthorizationException("Action not allowed");
//                }

                //AuthenticatedUser authenticatedUser = authenticator.authenticate(credentials);
                ctx.fireChannelRead(request);

                // User name logged with the remote socket address and authenticator classname for audit logging
//                if (authenticationSettings.enableAuditLog) {
//                    String address = ctx.channel().remoteAddress().toString();
//                    if (address.startsWith("/") && address.length() > 1) address = address.substring(1);
//                    final String[] authClassParts = authenticator.getClass().toString().split("[.]");
//                    auditLogger.info("User {} with address {} authenticated by {}",
//                            credentials.get(PROPERTY_USERNAME), address, authClassParts[authClassParts.length - 1]);
//                }
            } catch (AuthorizationException ae) {
                sendError(ctx, msg);
            }
        }
    }

    private void sendError(final ChannelHandlerContext ctx, final Object msg) {
        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN)).addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.release(msg);
    }
}
