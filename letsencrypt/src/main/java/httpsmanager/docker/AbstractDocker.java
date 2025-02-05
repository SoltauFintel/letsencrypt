package httpsmanager.docker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.pmw.tinylog.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;

public abstract class AbstractDocker {
    private final DockerClient docker;
    
    public AbstractDocker() {
        docker = createClient();
    }
    
    protected abstract DockerClient createClient();
    
    public List<String> getContainerNames(boolean all) {
        return getContainers(all).stream().map(c -> {
            if (c.getNames() != null && c.getNames().length > 0) {
                String ret = c.getNames()[0];
                if (ret.startsWith("/")) {
                    ret = ret.substring(1);
                }
                return ret + ", " + c.getState();
            }
            return "unknown";
        }).sorted().toList();
    }

    private List<Container> getContainers(boolean all) {
        return docker.listContainersCmd().withShowAll(all).exec();
    }

    public void removeContainer(String name, Boolean force) {
        try {
//            Logger.debug("Removing container '" + name + "' (force: " + force + ")");
            docker.removeContainerCmd(name).withForce(force).exec();
//            Logger.debug("Container '" + name + "' removed");
        } catch (Exception e) {
            Logger.error(e, "Error removing container '" + name + "'");
        }
    }
    
    public void pull(String image) {
        try {
        	if (!image.contains(":")) {
        		image += ":latest";
        	}
        	Logger.debug("Pulling image '" + image + "'");
            docker.pullImageCmd(image).exec(new PullImageResultCallback()).awaitCompletion();
        	Logger.debug("Image '" + image + "' pulled");
        } catch (Exception e) {
            Logger.error(e, "Error pulling image '" + image + "'");
        }
    }

    public void createAndStartContainer(String image, String name, String ports, String network, List<DockerVolume> volumes) {
        HostConfig hc = new HostConfig()
            .withRestartPolicy(RestartPolicy.alwaysRestart())
            .withLogConfig(new LogConfig(LogConfig.LoggingType.DEFAULT, getLogConfig()));

        // Ports
        List<ExposedPort> exposedPorts = new ArrayList<>();
        List<PortBinding> portBindings = new ArrayList<>();
        for (String port : ports.split(",")) {
            int p = Integer.parseInt(port);
            ExposedPort ep = ExposedPort.tcp(p);
            exposedPorts.add(ep);
            portBindings.add(new PortBinding(Binding.bindPort(p), ep));
        }
        hc = hc.withPortBindings(portBindings);
        
        if (volumes != null && !volumes.isEmpty()) {
            hc = hc.withBinds(volumes.stream().map(i -> i.getBind()).toList());
        }

        if (network != null && !network.isBlank()) {
            hc = hc.withNetworkMode(network);
        }

//        Logger.debug("Creating container '" + name + "' from image '" + image + "'...");
        docker.createContainerCmd(image)
            .withExposedPorts(exposedPorts)
            .withName(name)
            .withHostConfig(hc)
            .exec();
    
//        Logger.debug("Starting container '" + name + "'...");
        docker.startContainerCmd(name).exec();
//        Logger.debug("Container '" + name + "' started");
    }
    
    private Map<String, String> getLogConfig() {
        Map<String, String> ret = new HashMap<>();
        ret.put("max-size", "5m");
        ret.put("max-file", "5");
        return ret;
    }
    
    public String logs(String container) {
        StringBuffer sb = new StringBuffer();
        try {
            LogContainerCmd logContainerCmd = docker.logContainerCmd(container);
            logContainerCmd.withStdOut(true).withStdErr(true);
            logContainerCmd.exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    sb.append(item.toString());
                    sb.append("\n");
                }
            }).awaitCompletion(30, TimeUnit.SECONDS);
            return sb.toString();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class DockerVolume {
        private final String hostPath;
        private final String containerPath;
        
        public DockerVolume(String hostPath, String containerPath) {
            this.hostPath = hostPath;
            this.containerPath = containerPath;
        }

        public String getHostPath() {
            return hostPath;
        }

        public String getContainerPath() {
            return containerPath;
        }

        public Bind getBind() {
            return new Bind(hostPath, new Volume(containerPath), AccessMode.ro);
        }
    }
}
