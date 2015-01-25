package org.kantega.reststop.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.eclipse.jetty.server.Server;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Mojo(name = "run",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        requiresDirectInvocation = true,
        requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.PACKAGE)
public class RunMojo extends AbstractReststopMojo {

    @Parameter(defaultValue = "${path}")
    private String path;

    @Parameter(defaultValue = "${openProjectDir}")
    private boolean openProjectDir;

    @Override
    protected void afterServerStart(Server server, int port) throws MojoFailureException {
        try {
            openInBrowser("http://localhost:" + port);
            server.join();
        } catch (InterruptedException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void openInBrowser(String url) throws MojoFailureException {
        try {
            if (Desktop.isDesktopSupported()) {
                if (path != null) {
                    url += "/" + path;
                }
                DesktopApi.browse(new URI(url));
                if (openProjectDir) {
                    DesktopApi.open(mavenProject.getBasedir());
                }
            }

        } catch (URISyntaxException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    @Override
    protected List<Plugin> getPlugins() {
        List<Plugin> plugins = new ArrayList<>(super.getPlugins());

        if(mavenProject.getPackaging().equals("jar")) {
            Plugin projectPlugin = new Plugin(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion());
            projectPlugin.setSourceDirectory(mavenProject.getBasedir());
            plugins.add(projectPlugin);
        }

        addDevelopmentPlugins(plugins);


        return plugins;
    }

}
