package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.feature.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.pde.internal.*;
import org.eclipse.core.runtime.*;

public class BuildFeatureAction extends Action {
	public static final String LABEL = "FeatureEditor.BuildAction.label";
	BuildFeatureJarAction buildDelegate;
	FeatureEditor activeEditor;

public BuildFeatureAction() {
	setText(PDEPlugin.getResourceString(LABEL));
	buildDelegate = new BuildFeatureJarAction();

}
private void ensureContentSaved() {
	if (activeEditor.isDirty()) {
		ProgressMonitorDialog monitor =
			new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
		try {
			monitor.run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					activeEditor.doSave(monitor);
				}
			});
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
	}
}
public void run() {
	ensureContentSaved();
	buildDelegate.run(this);
}
public void setActiveEditor(FeatureEditor editor) {
	this.activeEditor = editor;
	buildDelegate.setActivePart(this, editor);
	IFeatureModel model = (IFeatureModel) editor.getModel();
	buildDelegate.setComponentFile((IFile) model.getUnderlyingResource());
}
}
