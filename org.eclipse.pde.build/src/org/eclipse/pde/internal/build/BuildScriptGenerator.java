/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.builder.*;

public class BuildScriptGenerator extends AbstractScriptGenerator {
	/**
	 * Indicates whether the assemble script should contain the archive
	 * generation statement.
	 */
	protected boolean generateArchive = true;
	/**
	 * Indicates whether scripts for a feature's children should be generated.
	 */
	protected boolean children = true;

	/**
	 * Source elements for script generation.
	 */
	protected String[] elements;

	/**
	 * Additional dev entries for the compile classpath.
	 */
	protected DevClassPathHelper devEntries;

	/**
	 * Plugin path. URLs that point where to find the plugins.
	 */
	protected String[] pluginPath;

	protected boolean recursiveGeneration = true;
	protected boolean signJars = false;
	protected boolean generateJnlp = false;

	/**
	 * flag indicating if the assemble script should be generated
	 */
	private boolean generateAssembleScript = true;

	/**
	 * flag indicating if the errors detected when the state is resolved must be reported or not.
	 * For example in releng mode we are interested in reporting the errors. It is the default. 
	 */
	private boolean reportResolutionErrors = true;
	
	/** flag indicating if missing properties file should be logged */
	private boolean ignoreMissingPropertiesFile = false;
	
	/**
	 * 
	 * @throws CoreException
	 */
	public void generate() throws CoreException {
		List plugins = new ArrayList(5);
		List features = new ArrayList(5);
		sortElements(features, plugins);

		// It is not required to filter in the two first generateModels, since
		// it is only for the building of a single plugin
		generateModels(plugins);
		generateFeatures(features);
	}

	/**
	 * Separate elements by kind.
	 */
	protected void sortElements(List features, List plugins) {
		for (int i = 0; i < elements.length; i++) {
			int index = elements[i].indexOf('@');
			String type = elements[i].substring(0, index);
			String element = elements[i].substring(index + 1);
			if (type.equals("plugin") || type.equals("fragment")) //$NON-NLS-1$ //$NON-NLS-2$
				plugins.add(element);
			else if (type.equals("feature")) //$NON-NLS-1$
				features.add(element);
		}
	}

	/**
	 * 
	 * @param models
	 * @throws CoreException
	 */
	protected void generateModels(List models) throws CoreException {
		for (Iterator iterator = models.iterator(); iterator.hasNext();) {
			ModelBuildScriptGenerator generator = new ModelBuildScriptGenerator();
			generator.setReportResolutionErrors(reportResolutionErrors);
			generator.setIgnoreMissingPropertiesFile(ignoreMissingPropertiesFile);
			//Filtering is not required here, since we are only generating the
			// build for a plugin or a fragment
			String model = (String) iterator.next();
			generator.setModelId(model);
			generator.setSignJars(signJars);
			generator.generate();
		}
	}

	/**
	 * 
	 * @param features
	 * @throws CoreException
	 */
	protected void generateFeatures(List features) throws CoreException {
		for (Iterator i = features.iterator(); i.hasNext();) {
			AssemblyInformation assemblageInformation = null;
			assemblageInformation = new AssemblyInformation();

			String featureId = (String) i.next();
			String versionId = null;
			int versionPosition = featureId.indexOf(":"); //$NON-NLS-1$
			if (versionPosition != -1) {
				versionId = featureId.substring(versionPosition + 1);
				featureId = featureId.substring(0, versionPosition);
			}
			FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator(featureId, versionId, assemblageInformation);
			generator.setGenerateIncludedFeatures(this.recursiveGeneration);
			generator.setAnalyseChildren(this.children);
			generator.setSourceFeatureGeneration(false);
			generator.setBinaryFeatureGeneration(true);
			generator.setScriptGeneration(true);
			generator.setPluginPath(pluginPath);
			generator.setBuildSiteFactory(null);
			generator.setDevEntries(devEntries);
			generator.setSourceToGather(new SourceFeatureInformation());
			generator.setCompiledElements(generator.getCompiledElements());
			generator.setBuildingOSGi(isBuildingOSGi());
			generator.includePlatformIndependent(true);
			generator.setReportResolutionErrors(reportResolutionErrors);
			generator.setIgnoreMissingPropertiesFile(ignoreMissingPropertiesFile);
			generator.setSignJars(signJars);
			generator.setGenerateJnlp(generateJnlp);			
			generator.generate();

			if (generateAssembleScript == true) {
				AssembleScriptGenerator assembler = new AssembleScriptGenerator(workingDirectory, assemblageInformation, featureId, null);
				assembler.setSignJars(signJars);
				assembler.setGenerateJnlp(generateJnlp);			
				assembler.generate();
			}
		}
	}

	public void setGenerateArchive(boolean generateArchive) {
		this.generateArchive = generateArchive;
	}

	/**
	 * 
	 * @param children
	 */
	public void setChildren(boolean children) {
		this.children = children;
	}

	/**
	 * 
	 * @param devEntries
	 */
	public void setDevEntries(String devEntries) {
		if (devEntries != null)
			this.devEntries = new DevClassPathHelper(devEntries);
	}

	/**
	 * 
	 * @param elements
	 */
	public void setElements(String[] elements) {
		this.elements = elements;
	}

	public void setPluginPath(String[] pluginPath) {
		this.pluginPath = pluginPath;
	}

	/**
	 * Sets the recursiveGeneration.
	 * 
	 * @param recursiveGeneration
	 *            The recursiveGeneration to set
	 */
	public void setRecursiveGeneration(boolean recursiveGeneration) {
		this.recursiveGeneration = recursiveGeneration;
	}

	/**
	 * @param generateAssembleScript
	 *            The generateAssembleScript to set.
	 */
	public void setGenerateAssembleScript(boolean generateAssembleScript) {
		this.generateAssembleScript = generateAssembleScript;
	}
	/**
	 * @param value The reportResolutionErrors to set.
	 */
	public void setReportResolutionErrors(boolean value) {
		this.reportResolutionErrors = value;
	}

	/**
	 * @param value The ignoreMissingPropertiesFile to set.
	 */
	public void setIgnoreMissingPropertiesFile(boolean value) {
		ignoreMissingPropertiesFile = value;
	}

	public void setSignJars(boolean value) {
		signJars = value;
	}

	public void setGenerateJnlp(boolean value) {
		generateJnlp = value;
	}
}
