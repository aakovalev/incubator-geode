/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gemstone.gemfire.cache.client.internal;

import com.gemstone.gemfire.internal.cache.tier.MessageType;
import com.gemstone.gemfire.internal.cache.tier.sockets.Message;

import java.io.EOFException;

/**
 * Tell a server that a connection is being closed
 * @since 5.7
 */
public class CloseConnectionOp {
  /**
   * Tell a server that a connection is being closed
   * @param con the connection that is being closed
   * @param keepAlive whether to keep the proxy alive on the server
   */
  public static void execute(Connection con, boolean keepAlive)
    throws Exception
  {
    AbstractOp op = new CloseConnectionOpImpl(keepAlive);
    try {
      con.execute(op);
    } catch (EOFException e) {
      // expected
    }
  }
                                                               
  private CloseConnectionOp() {
    // no instances allowed
  }
  
  private static class CloseConnectionOpImpl extends AbstractOp {
    /**
     * @throws com.gemstone.gemfire.SerializationException if serialization fails
     */
    public CloseConnectionOpImpl(boolean keepAlive)  {
      super(MessageType.CLOSE_CONNECTION, 1);
      getMessage().addRawPart(new byte[]{(byte)(keepAlive?1:0)}, false);
    }
     @Override
    protected void processSecureBytes(Connection cnx, Message message)
        throws Exception {
    }

    @Override
    protected boolean needsUserId() {
      return false;
    }

    @Override
    protected void sendMessage(Connection cnx) throws Exception {
      getMessage().clearMessageHasSecurePartFlag();
      getMessage().send(false);
    }

    @Override
    protected Object processResponse(Message msg) throws Exception {
      // CloseConnectionOp doesn't return anything - we wait for a response
      // so that we know that the server has processed the request before
      // we return from execute();
      return null;
    }

    @Override
    protected boolean isErrorResponse(int msgType) {
      return false;
    }
    @Override  
    protected long startAttempt(ConnectionStats stats) {
      return stats.startCloseCon();
    }
    @Override  
    protected void endSendAttempt(ConnectionStats stats, long start) {
      stats.endCloseConSend(start, hasFailed());
    }
    @Override  
    protected void endAttempt(ConnectionStats stats, long start) {
      stats.endCloseCon(start, hasTimedOut(), hasFailed());
    }
  }
}
