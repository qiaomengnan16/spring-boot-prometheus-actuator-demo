package com.demo;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@SpringBootApplication
public class App implements InitializingBean {

    @Autowired
    private ApplicationContext applicationContext;

    @PrometheusThreadPool
    @Bean("my-executor")
    public ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(10, 30, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50), new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("thread-pool-" + counter.getAndIncrement());
                return thread;
            }
        }, new ThreadPoolExecutor.CallerRunsPolicy());
    }


    public static void registerThreadPool(String poolName, ThreadPoolExecutor executor) {
        List<Tag> tags = Arrays.asList(new ImmutableTag("poolName", poolName));
        Metrics.gauge("thread_pool_core_size", tags, executor, ThreadPoolExecutor::getCorePoolSize);
        Metrics.gauge("thread_pool_largest_size", tags, executor, ThreadPoolExecutor::getLargestPoolSize);
        Metrics.gauge("thread_pool_max_size", tags, executor, ThreadPoolExecutor::getMaximumPoolSize);
        Metrics.gauge("thread_pool_active_size", tags, executor, ThreadPoolExecutor::getActiveCount);
        Metrics.gauge("thread_pool_thread_count", tags, executor, ThreadPoolExecutor::getPoolSize);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(PrometheusThreadPool.class);
        for (String beanName : beanNames) {
            registerThreadPool(beanName, (ThreadPoolExecutor) applicationContext.getBean(beanName));
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
