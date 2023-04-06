package com.redhat.fuse.quickstarts;

import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import me.snowdrop.boot.narayana.core.jdbc.PooledXADataSourceWrapper;
import me.snowdrop.boot.narayana.core.properties.NarayanaProperties;
import org.apache.camel.component.jms.JmsComponent;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

@Configuration
public class ApplicationConfiguration {

    @Value("${database.host}")
    private String databaseHost;

    @Value("${database.port}")
    private String databasePort;

    @Value("${database.name}")
    private String databaseName;

    @Value("${database.username}")
    private String databaseUsername;

    @Value("${database.password}")
    private String databasePassword;

    @Value("${database.platform}")
    private String databasePlatform;


    @Bean(name = "jms-component")
    public JmsComponent jmsComponent(ConnectionFactory xaJmsConnectionFactory, PlatformTransactionManager jtaTransactionManager) {
        JmsComponent jms = new JmsComponent();
        jms.setConnectionFactory(xaJmsConnectionFactory);
        jms.setTransactionManager(jtaTransactionManager);
        jms.setTransacted(true);

        return jms;
    }

    @Bean
    public DataSource postgresXADataSource(TransactionManager transactionManager, XARecoveryModule xaRecoveryModule) throws Exception {
        PGXADataSource pgXaDataSource = new PGXADataSource();
        pgXaDataSource.setURL("jdbc:" + databasePlatform + "://" + databaseHost + ":" + databasePort + "/"+ databaseName);
        pgXaDataSource.setUser(databaseUsername);
        pgXaDataSource.setPassword(databasePassword);
        PooledXADataSourceWrapper wrapper = new PooledXADataSourceWrapper(new NarayanaProperties(), xaRecoveryModule, transactionManager);
        return wrapper.wrapDataSource(pgXaDataSource);
    }
}
