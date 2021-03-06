/*
 * Copyright 2011 buddycloud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.buddycloud.pusher.handler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.PacketError.Condition;

import com.buddycloud.pusher.db.DataSource;

/**
 * @author Abmar
 * 
 */
public class UnregisterQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "jabber:iq:register";

	/**
	 * @param xmppComponent 
	 * @param namespace
	 * @param properties
	 */
	public UnregisterQueryHandler(Properties properties, DataSource dataSource) {
		super(NAMESPACE, properties, dataSource);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp
	 * .packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element removeEl = queryElement.element("remove");
		
		if (removeEl == null) {
			return createFeatureNotImplementedError(iq);
		}
		
		String from = iq.getFrom().toBareJID();
		try {
			removeSubscriber(from);
		} catch (Exception e) {
			return createInternalServerError(iq);
		}
		
		return IQ.createResultIQ(iq);
	}
	
	private void removeSubscriber(String jid) throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = getDataSource().prepareStatement(
					"DELETE FROM notification_settings WHERE jid = ?", jid);
			statement.execute();
		} catch (SQLException e) {
			getLogger().error("Could not delete user [" + jid + "].", e);
			throw e;
		} finally {
			DataSource.close(statement);
		}
	}

	private IQ createFeatureNotImplementedError(IQ iq) {
		IQ result = IQ.createResultIQ(iq);
		result.setType(Type.error);
		PacketError pe = new PacketError(
				Condition.feature_not_implemented,
				org.xmpp.packet.PacketError.Type.cancel);
		result.setError(pe);
		return result;
	}
	
	private IQ createInternalServerError(IQ iq) {
		IQ result = IQ.createResultIQ(iq);
		result.setType(Type.error);
		PacketError pe = new PacketError(
				Condition.internal_server_error,
				org.xmpp.packet.PacketError.Type.cancel);
		result.setError(pe);
		return result;
	}
}
