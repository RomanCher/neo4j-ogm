package org.springframework.data.neo4j.repository;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;

public class GraphRepositoryFactoryBean<S extends Repository<T, Long>, T> extends TransactionalRepositoryFactoryBeanSupport<S, T, Long> {

    @Autowired
    private Session session;

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return new GraphRepositoryFactory<S, T>(session);
    }

}
