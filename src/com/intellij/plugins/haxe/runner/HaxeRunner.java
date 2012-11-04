package com.intellij.plugins.haxe.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeRunner extends DefaultProgramRunner {
  public static final String HAXE_RUNNER_ID = "HaxeRunner";

  public static final RunProfileState EMPTY_RUN_STATE = new RunProfileState() {
    public ExecutionResult execute(final Executor executor, @NotNull final ProgramRunner runner) throws ExecutionException {
      return null;
    }

    public RunnerSettings getRunnerSettings() {
      return null;
    }

    public ConfigurationPerRunnerSettings getConfigurationSettings() {
      return new ConfigurationPerRunnerSettings(HAXE_RUNNER_ID, null);
    }
  };

  @NotNull
  @Override
  public String getRunnerId() {
    return HAXE_RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof HaxeApplicationConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(Project project,
                                           Executor executor,
                                           RunProfileState state,
                                           RunContentDescriptor contentToReuse,
                                           ExecutionEnvironment env) throws ExecutionException {
    final HaxeApplicationConfiguration configuration = (HaxeApplicationConfiguration)env.getRunProfile();
    final Module module = configuration.getConfigurationModule().getModule();

    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("no.module.for.run.configuration", configuration.getName()));
    }

    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);

    if (settings.isUseNmmlToBuild()) {
      final NMERunningState nmeRunningState = new NMERunningState(env, module, false);
      return super.doExecute(project, executor, nmeRunningState, contentToReuse, env);
    }

    if (configuration.isCustomFileToLaunch() && "n".equalsIgnoreCase(FileUtil.getExtension(configuration.getCustomFileToLaunchPath()))) {
      final NekoRunningState nekoRunningState = new NekoRunningState(env, module, configuration.getCustomFileToLaunchPath());
      return super.doExecute(project, executor, nekoRunningState, contentToReuse, env);
    }

    if (configuration.isCustomExecutable()) {
      final String filePath = configuration.isCustomFileToLaunch()
                              ? configuration.getCustomFileToLaunchPath()
                              : getOutputFilePath(module, settings);
      return super.doExecute(project, executor, new CommandLineState(env) {
        @NotNull
        @Override
        protected ProcessHandler startProcess() throws ExecutionException {
          final GeneralCommandLine commandLine = new GeneralCommandLine();
          commandLine.setWorkDirectory(PathUtil.getParentPath(module.getModuleFilePath()));
          commandLine.setExePath(configuration.getCustomExecutablePath());
          commandLine.addParameter(filePath);

          final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject());
          setConsoleBuilder(consoleBuilder);

          return new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
        }
      }, contentToReuse, env);
    }

    if (configuration.isCustomFileToLaunch()) {
      launchUrl(configuration.getCustomFileToLaunchPath());
      return null;
    }

    if (settings.getHaxeTarget() == HaxeTarget.FLASH) {
      final String url = getOutputFilePath(module, settings);
      launchUrl(url);
      return null;
    }

    if (settings.getHaxeTarget() != HaxeTarget.NEKO) {
      throw new ExecutionException(HaxeBundle.message("haxe.run.wrong.target", settings.getHaxeTarget()));
    }

    final NekoRunningState nekoRunningState = new NekoRunningState(env, module, null);
    return super.doExecute(project, executor, nekoRunningState, contentToReuse, env);
  }

  private String getOutputFilePath(Module module, HaxeModuleSettings settings) {
    FileDocumentManager.getInstance().saveAllDocuments();
    final CompilerModuleExtension model = CompilerModuleExtension.getInstance(module);
    assert model != null;
    return model.getCompilerOutputUrl() + "/release/" + settings.getOutputFileName();
  }

  public static void launchUrl(String urlOrPath) {
    if (Desktop.isDesktopSupported()) {
      final Desktop desktop = Desktop.getDesktop();
      if (BrowserUtil.isAbsoluteURL(urlOrPath)) {
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
          try {
            desktop.browse(BrowserUtil.getURL(urlOrPath).toURI());
            return;
          }
          catch (IOException ignored) {/*ignored*/}
          catch (URISyntaxException ignored) {/*ignored*/}
        }
      }
      else {
        if (desktop.isSupported(Desktop.Action.OPEN)) {
          try {
            desktop.open(new File(urlOrPath));
            return;
          }
          catch (IOException ignored) {/*ignored*/}
          catch (IllegalArgumentException ignored) {/*ignored*/}
        }
      }
    }

    BrowserUtil.launchBrowser(urlOrPath);
  }
}
