/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.plugin.IWritableDelimiter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

/**
 * PDEDragAdapter
 *
 */
public class PDEDragAdapter implements DragSourceListener, IPDESourceParticipant {

	private IPDEDragParticipant fParticipant;
	
	// Needs to be static to allow dragging objects from one viewer to another
	private static Object[] fSourceObjects;

	/**
	 * @param participant
	 */
	public PDEDragAdapter(IPDEDragParticipant participant) {
		fParticipant = participant;
		resetSourceObjects();
	}

	/**
	 * @param objects
	 */
	protected void setSourceObjects(Object[] objects) {
		fSourceObjects = objects;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDESourceParticipant#getSourceObjects()
	 */
	public Object[] getSourceObjects() {
		return fSourceObjects;
	}
	
	/**
	 * 
	 */
	protected void resetSourceObjects() {
		fSourceObjects = null;
	}
	
	/**
	 * @return
	 */
	protected boolean isCopyOperationSupported() {
		if ((fParticipant.getSupportedDNDOperations() & DND.DROP_COPY) == DND.DROP_COPY) {
			return true;
		} 
		return false;
	}
	
	/**
	 * @return
	 */
	protected boolean isMoveOperationSupported() {
		if ((fParticipant.getSupportedDNDOperations() & DND.DROP_MOVE) == DND.DROP_MOVE) {
			return true;
		} 
		return false;
	}	
	
	/**
	 * @return
	 */
	protected boolean isLinkOperationSupported() {
		if ((fParticipant.getSupportedDNDOperations() & DND.DROP_LINK) == DND.DROP_LINK) {
			return true;
		} 
		return false;
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public void dragFinished(DragSourceEvent event) {
		// Nothing to do if drag event is not enabled
		if (event.doit == false) {
			return;
		}
		// TODO: MP: DND: BUG: On text transfer the object is removed, need to force operation into a copy 
		// TODO: MP: DND: BUG: On text transfer no object selected in tree - related to above
		// If the move operation was performed, then remove the original source objects
		if (event.detail == DND.DROP_MOVE) {
			fParticipant.doDragRemove(getSourceObjects());
		}
	}
	
	/**
	 * @param event
	 */
	protected void validateDrag(DragSourceEvent event) {
		// Nothing to do if drag event is not enabled
		if (event.doit == false) {
			return;
		}
		// Get the source objects (selection in tree);
		// TODO: MP: DND: Adapt for tables in the future
		// Get the source of the event
		Object source = event.getSource();
		// Ensure we have a drag source
		if ((source instanceof DragSource) == false) {
			event.doit = false;
			return;			
		}
		// Get the control of the source
		Control control = ((DragSource)source).getControl();
		// Get the items selected in the tree or table
		Item[] items = null;
		if (control instanceof Tree) {
			// Get the tree's selection
			items = ((Tree)control).getSelection();
		} else if (control instanceof Table) {
			// Get the table's selection
			items = ((Table)control).getSelection();
		} else {
			event.doit = false;
			return;		
		}
		// Ensure there are selected objects
		if (items.length == 0) {
			event.doit = false;
			return;			
		}
		// Create the container for the source objects
		Object[] sourceObjects = new Object[items.length];
		// Store all source objects
		for (int i = 0; i < items.length; i++) {
			sourceObjects[i] = items[i].getData();
		}		
		// Store the source objects for later use
		setSourceObjects(sourceObjects);
		// event.doit is true by default
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public void dragSetData(DragSourceEvent event) {
		// Check to see if the drag event is valid
		if (event.doit == false) {
			return;
		}
		// Determine data required by target
		if (ModelDataTransfer.getInstance().isSupportedType(event.dataType)) {
			// Model transfer target
			// e.g. Tree viewer, Table viewer
			event.data = getSourceObjects();
		} else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
			// Text transfer target
			// e.g. Word, Source page
			event.data = createTextualRepresentation();
		}
	}

	/**
	 * @return
	 */
	protected Object createTextualRepresentation() {
		String textualRepresentation = null;
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		boolean firstIteration = true;
		Object[] sourceObjects = getSourceObjects();
		// Serialize each source object to text
		for (int i = 0; i < sourceObjects.length; i++) {
			Object object = sourceObjects[i];
			if (object instanceof IWritable) {
				if ((firstIteration == false) &&
						(object instanceof IWritableDelimiter)) {
					// Add a customized delimiter in between all serialized
					// objects to format the text representation
					((IWritableDelimiter)object).writeDelimeter(printWriter);
				}
				// Write the textual representation of the object
				((IWritable)object).write("", printWriter); //$NON-NLS-1$
			} else if (object instanceof String) {
				// Delimiter is always a newline
				printWriter.println((String)object);
			}
			firstIteration = false;
		}
		// Flush the writer
		printWriter.flush();
		// Get the String representation
		textualRepresentation = stringWriter.toString();
		// Close the print writer
		printWriter.close();
		// Close the string writer
		try {
			stringWriter.close();
		} catch (IOException e) {
			// Ignore
		}
		return textualRepresentation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public void dragStart(DragSourceEvent event) {
		// Clear the previous source objects
		// New drag starting
		resetSourceObjects();
		// Validate the drag event
		validateDrag(event);
		// Check to see if the drag event is valid
		if (event.doit == false) {
			return;
		}
		// TODO: MP: DND: Want to support some operations for some items - not all or nothing
		// Disable the drag event if the copy operation is supported; but, the
		// selection cannot be copied
		if (isCopyOperationSupported() && 
				(fParticipant.canDragCopy(getSourceObjects()) == false)) {
			event.doit = false;
			return;			
		}
		// Disable the drag event if the move operation is supported; but, the
		// selection cannot be cut
		if (isMoveOperationSupported() && 
				(fParticipant.canDragMove(getSourceObjects()) == false)) {
			event.doit = false;
			return;			
		}
		// Disable the drag event if the link operation is supported; but, the
		// selection cannot be linked
		if (isLinkOperationSupported() && 
				(fParticipant.canDragLink(getSourceObjects()) == false)) {
			event.doit = false;
			return;			
		}
		// Drag event is enabled
	}

}
