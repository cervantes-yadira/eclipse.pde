/**********************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.builder.FeatureBuildScriptGenerator;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IIncludedFeatureReference;
import org.eclipse.update.core.model.IncludedFeatureReferenceModel;

public class PackagerBuildScriptGenerator extends FeatureBuildScriptGenerator {
	boolean zipWithEclipse = false;
	private String packagingPropertiesLocation;
	private String[] featureList;
	private String[] rootFiles;
	private String[] rootDirs;
	private String outputFormat = "zip"; //$NON-NLS-1$
	private String[] ignoredFeatures;

	public PackagerBuildScriptGenerator() {
		super();
	}

	public PackagerBuildScriptGenerator(String featureId, AssemblyInformation assemblageInformation) throws CoreException {
		super(featureId, null, assemblageInformation);
	}

	public void run() throws CoreException {
		AssemblyInformation assemblageInformation = new AssemblyInformation();

		for (int i = 0; i < featureList.length; i++) {
			FeatureBuildScriptGenerator generator = new PackagerBuildScriptGenerator(featureList[i], assemblageInformation);
			generator.setGenerateIncludedFeatures(true);
			generator.setAnalyseChildren(true);
			generator.setSourceFeatureGeneration(false);
			generator.setBinaryFeatureGeneration(true);
			generator.setScriptGeneration(false);
			generator.setPluginPath(pluginPath);
			generator.setBuildSiteFactory(siteFactory);
			generator.setDevEntries(devEntries);
			generator.setCompiledElements(getCompiledElements());
			generator.setBuildingOSGi(isBuildingOSGi());
			setFeature(featureList[i]);
			generator.generate();
		}

		removeIgnoredFeatures(assemblageInformation);
		PackagingConfigScriptGenerator configAssembler = new PackagingConfigScriptGenerator();
		Config config = (Config) getConfigInfos().get(0);
		configAssembler.initialize(workingDirectory, DEFAULT_ASSEMBLE_FILENAME, "", config, assemblageInformation.getPlugins(config), assemblageInformation.getFeatures(config), true); //$NON-NLS-1$ //Here the last arg is true because we do not have the root info while packaging
		configAssembler.setPackagingPropertiesLocation(packagingPropertiesLocation);
		configAssembler.rootFiles(rootFiles);
		configAssembler.rootDirs(rootDirs);
		configAssembler.setOutput(outputFormat);
		configAssembler.generate();

	}

	private void removeIgnoredFeatures(AssemblyInformation toAssemble) {
		if (ignoredFeatures == null)
			return;
		for (int i = 0; i < ignoredFeatures.length; i++) {
			try {
				toAssemble.removeFeature((Config) getConfigInfos().get(0), getSite(false).findFeature(ignoredFeatures[i], null, true));
			} catch (CoreException e) {
				//Ignore
			}
		}
	}

	protected void generateIncludedFeatureBuildFile() throws CoreException {
		IIncludedFeatureReference[] referencedFeatures = feature.getIncludedFeatureReferences();
		for (int i = 0; i < referencedFeatures.length; i++) {
			String featureId = ((IncludedFeatureReferenceModel) referencedFeatures[i]).getFeatureIdentifier();
			FeatureBuildScriptGenerator generator = new PackagerBuildScriptGenerator(featureId, assemblyData);
			generator.setGenerateIncludedFeatures(true);
			generator.setAnalyseChildren(true);
			generator.setSourceFeatureGeneration(false);
			generator.setBinaryFeatureGeneration(true);
			generator.setScriptGeneration(false);
			generator.setPluginPath(pluginPath);
			generator.setBuildSiteFactory(siteFactory);
			generator.setDevEntries(devEntries);
			generator.setCompiledElements(getCompiledElements());
			generator.setBuildingOSGi(isBuildingOSGi());
			try {
				generator.generate();
			} catch (CoreException exception) {
				//If the referenced feature is not optional, there is a real problem and the exception is re-thrown. 
				if (exception.getStatus().getCode() == EXCEPTION_FEATURE_MISSING && !referencedFeatures[i].isOptional())
					throw exception;
			}
		}
	}

	public void setPropertyFile(String propertyFile) {
		packagingPropertiesLocation = propertyFile;
	}

	public void setFeatureList(String features) {
		featureList = Utils.getArrayFromString(features, ","); //$NON-NLS-1$
	}

	public void setRootFiles(String[] rootFiles) {
		this.rootFiles = rootFiles;
	}

	public void setRootDirs(String[] rootDirs) {
		this.rootDirs = rootDirs;
	}

	public void setOutput(String format) { //TODO To rename
		this.outputFormat = format;
	}

	public void setIgnoredFeatures(String[] features) {
		ignoredFeatures = features;
	}
	
	protected void collectElementToAssemble(IFeature featureToCollect) throws CoreException {
		if (assemblyData == null)
			return;
		List correctConfigs = selectConfigs(featureToCollect);
		// Here, we could sort if the feature is a common one or not by
		// comparing the size of correctConfigs
		for (Iterator iter = correctConfigs.iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			assemblyData.addFeature(config, feature);
		}
	}
}