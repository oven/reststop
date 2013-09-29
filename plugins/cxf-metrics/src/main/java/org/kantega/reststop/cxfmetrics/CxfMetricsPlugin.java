package org.kantega.reststop.cxfmetrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.jaxws22.EndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.kantega.reststop.api.ReststopPluginManager;
import org.kantega.reststop.cxf.api.DefaultCxfPluginPlugin;
import org.kantega.reststop.metrics.MetricsReststopPlugin;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class CxfMetricsPlugin extends DefaultCxfPluginPlugin {


    private final ReststopPluginManager pluginManager;

    public CxfMetricsPlugin(ReststopPluginManager pluginManager) {

        this.pluginManager = pluginManager;
    }

    @Override
    public void customizeEndpoint(Endpoint endpoint) {
        EndpointImpl e = (EndpointImpl) endpoint;

        MetricRegistry registry = pluginManager.getPlugins(MetricsReststopPlugin.class).iterator().next().getMetricRegistry();

        e.getServer().getEndpoint().getInInterceptors().add(new TimingBeforeInterceptor(Phase.RECEIVE));
        e.getServer().getEndpoint().getOutInterceptors().add(new TimingAfterInterceptor(Phase.SEND, registry));
    }

    private class TimingBeforeInterceptor extends AbstractPhaseInterceptor<Message> {
        public TimingBeforeInterceptor(String phase) {
            super(phase);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            message.getExchange().put("time_before", System.nanoTime());
        }
    }

    private class TimingAfterInterceptor extends AbstractPhaseInterceptor<Message> {

        private final MetricRegistry registry;

        private TimingAfterInterceptor(String phase, MetricRegistry registry) {
            super(phase);
            this.registry = registry;
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            Long time_before = (Long) message.getExchange().get("time_before");
            if(time_before != null) {
                QName operation = (QName) message.get(Message.WSDL_OPERATION);
                QName service = (QName) message.get(Message.WSDL_SERVICE);

                String name = operation.toString();
                Timer timer;
                if(!registry.getTimers().containsKey(name)) {
                    timer = registry.register(name, new Timer());
                } else {
                    timer = registry.getTimers().get(name);
                }

                timer.update(System.nanoTime() - time_before, TimeUnit.NANOSECONDS);

            }
        }
    }
}