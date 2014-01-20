/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.runner.debugger.hxcpp;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HXCPPRemoteRunConfigurationType implements ConfigurationType {
  private final HXCPPFactory configurationFactory;

  public HXCPPRemoteRunConfigurationType() {
    configurationFactory = new HXCPPFactory(this);
  }

  public static HXCPPRemoteRunConfigurationType getInstance() {
    return ContainerUtil.findInstance(Extensions.getExtensions(CONFIGURATION_TYPE_EP), HXCPPRemoteRunConfigurationType.class);
  }

  public String getDisplayName() {
    return HaxeBundle.message("hxcpp.remote.debug.configuration.name");
  }

  public String getConfigurationTypeDescription() {
    return HaxeBundle.message("hxcpp.remote.debug.configuration.name");
  }

  public Icon getIcon() {
    return icons.HaxeIcons.Haxe_16;
  }

  @NotNull
  public String getId() {
    return "HXCPPRemoteRunConfigurationType";
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{configurationFactory};
  }

  public static class HXCPPFactory extends ConfigurationFactory {

    public HXCPPFactory(ConfigurationType type) {
      super(type);
    }

    public RunConfiguration createTemplateConfiguration(Project project) {
      final String name = HaxeBundle.message("hxcpp.remote.debug.configuration.name");
      return new HXCPPRemoteDebugConfiguration(name, project, getInstance());
    }
  }
}
