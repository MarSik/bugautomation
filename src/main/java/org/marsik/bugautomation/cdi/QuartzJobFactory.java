package org.marsik.bugautomation.cdi;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.weld.environment.se.WeldContainer;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/* Based on http://vijaykiran.com/2013/01/a-quick-way-to-integrate-seam3-cdi-and-quartz-jobs/ */
@ApplicationScoped
@ManagedBean
public class QuartzJobFactory implements JobFactory {
    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        final JobDetail jobDetail = bundle.getJobDetail();
        final Class<? extends Job> jobClass = jobDetail.getJobClass();

        return getBean(jobClass);
    }

    private Job getBean(Class jobClazz) {
        return (Job) WeldContainer.instance(WeldContainer.getRunningContainerIds().get(0)).select(jobClazz).get();
    }
}
