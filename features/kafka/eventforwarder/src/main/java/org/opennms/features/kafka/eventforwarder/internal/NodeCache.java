package org.opennms.features.kafka.eventforwarder.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionOperations;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Maintains a cache of OnmsNode and OnmsCategories. The required node is fetched from the database
 * if it does not exist in the cache
 * 
 * @author sa029738
 *
 */
public class NodeCache
{

    private static final Logger            LOG      = LoggerFactory.getLogger( NodeCache.class );

    private long                           MAX_SIZE = 10000;
    private long                           MAX_TTL  = 5;                                         // Minutes

    private volatile NodeDao               nodeDao;
    private volatile TransactionOperations transactionOperations;

    private LoadingCache<Long, OnmsNode>   cache    = null;

    private OnmsNode                       onmsNode = null;

    public NodeCache ()
    {
    }

    public void init()
    {
        if ( cache == null )
        {
            LOG.info( "initializing node data cache (TTL=" + MAX_TTL + "m, MAX_SIZE=" + MAX_SIZE + ")" );
            CacheBuilder cacheBuilder = CacheBuilder.newBuilder();
            if ( MAX_TTL > 0 )
            {
                cacheBuilder.expireAfterWrite( MAX_TTL, TimeUnit.MINUTES );
            }
            if ( MAX_SIZE > 0 )
            {
                cacheBuilder.maximumSize( MAX_SIZE );
            }

            cache = cacheBuilder.build( new CacheLoader<Long, OnmsNode>() {
                @Override
                public OnmsNode load( Long key ) throws NodeNotFoundException
                {
                    OnmsNode node = getNodeAndCategoryInfo( key );
                    if ( node != null )
                        return node;
                    else
                    {
                        throw new NodeNotFoundException( "Node with id: " + key
                                        + " does not exist in the database. Returning null and not storing key( " + key
                                        + " ) in cache" );
                    }
                }
            } );
        }
    }

    public OnmsNode getEntry( Long key ) throws NodeNotFoundException
    {
        try
        {
            return cache.get( key );
        }
        catch ( ExecutionException e )
        {
            throw new NodeNotFoundException( e.getCause() );
        }
    }

    public void refreshEntry( Long key )
    {
        LOG.debug( "refreshing node cache entry: " + key );
        cache.refresh( key );
    }

    private OnmsNode getNodeAndCategoryInfo( Long nodeId )
    {
        // safety check
        if ( nodeId != null )
        {
            LOG.debug( "Fetching node data from database into cache" );

            // wrap in a transaction so that Hibernate session is bound and getCategories works
            transactionOperations.execute( new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult( TransactionStatus transactionStatus )
                {
                    onmsNode = nodeDao.get( nodeId.intValue() );
                    if ( onmsNode != null )
                    {
                        onmsNode.setCategories( onmsNode.getCategories() );
                        LOG.debug( "Categories size fetched: " + onmsNode.getCategories().size() );
                    }
                }
            } );

        }
        return onmsNode;
    }

    /* Getters and setters */
    public long getMAX_SIZE()
    {
        return MAX_SIZE;
    }

    public void setMAX_SIZE( long mAX_SIZE )
    {
        MAX_SIZE = mAX_SIZE;
    }

    public long getMAX_TTL()
    {
        return MAX_TTL;
    }

    public void setMAX_TTL( long mAX_TTL )
    {
        MAX_TTL = mAX_TTL;
    }

    public NodeDao getNodeDao()
    {
        return nodeDao;
    }

    public void setNodeDao( NodeDao nodeDao )
    {
        this.nodeDao = nodeDao;
    }

    public TransactionOperations getTransactionOperations()
    {
        return transactionOperations;
    }

    public void setTransactionOperations( TransactionOperations transactionOperations )
    {
        this.transactionOperations = transactionOperations;
    }
}
