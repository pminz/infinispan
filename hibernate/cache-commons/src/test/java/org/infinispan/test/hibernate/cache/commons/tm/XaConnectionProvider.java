package org.infinispan.test.hibernate.cache.commons.tm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Stoppable;
import org.infinispan.test.hibernate.cache.commons.util.TestingUtil;

/**
 * XaConnectionProvider.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class XaConnectionProvider implements ConnectionProvider, Stoppable {
	private final ConnectionProvider actualConnectionProvider;
	private boolean isTransactional;

	public XaConnectionProvider() {
		this(TestingUtil.buildConnectionProvider());
	}

	public XaConnectionProvider(ConnectionProvider connectionProvider) {
		this.actualConnectionProvider = connectionProvider;
	}

	public ConnectionProvider getActualConnectionProvider() {
		return actualConnectionProvider;
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return XaConnectionProvider.class.isAssignableFrom( unwrapType ) ||
            ConnectionProvider.class == unwrapType ||
				actualConnectionProvider.getClass().isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( XaConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( ConnectionProvider.class.isAssignableFrom( unwrapType ) ||
				actualConnectionProvider.getClass().isAssignableFrom( unwrapType ) ) {
			return (T) getActualConnectionProvider();
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	public void configure(Properties props) throws HibernateException {
	}

	public Connection getConnection() throws SQLException {
		XaTransactionImpl currentTransaction = XaTransactionManagerImpl.getInstance().getCurrentTransaction();
		if ( currentTransaction == null ) {
			isTransactional = false;
			return actualConnectionProvider.getConnection();
		}
		else {
			isTransactional = true;
			Connection connection = currentTransaction.getEnlistedConnection();
			if ( connection == null ) {
				connection = actualConnectionProvider.getConnection();
				currentTransaction.enlistConnection( connection, actualConnectionProvider );
			}
			return connection;
		}
	}

	public void closeConnection(Connection conn) throws SQLException {
		if ( !isTransactional ) {
			actualConnectionProvider.closeConnection(conn);
		}
	}

	@Override
	public void stop() {
		if ( actualConnectionProvider instanceof Stoppable ) {
			((Stoppable) actualConnectionProvider).stop();
		}
	}

	public boolean supportsAggressiveRelease() {
		return actualConnectionProvider.supportsAggressiveRelease();
	}
}
