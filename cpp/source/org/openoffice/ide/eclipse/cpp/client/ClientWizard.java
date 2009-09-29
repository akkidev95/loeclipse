/*************************************************************************
 *
 * The Contents of this file are made available subject to the terms of
 * the GNU Lesser General Public License Version 2.1
 *
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * Copyright 2009 by Novell, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 * 
 * The Initial Developer of the Original Code is: Cédric Bosdonnat.
 *
 * Copyright: 2009 by Novell, Inc.
 *
 * All Rights Reserved.
 * 
 ************************************************************************/
package org.openoffice.ide.eclipse.cpp.client;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.MessageFormat;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.ui.wizards.CCProjectWizard;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IWorkbenchPage;
import org.openoffice.ide.eclipse.core.PluginLogger;
import org.openoffice.ide.eclipse.core.model.config.IOOo;
import org.openoffice.ide.eclipse.core.model.config.ISdk;
import org.openoffice.ide.eclipse.core.utils.WorkbenchHelper;
import org.openoffice.ide.eclipse.cpp.Activator;
import org.openoffice.ide.eclipse.cpp.CppProjectHandler;

/**
 * Class for the C++ UNO Client wizard.
 * 
 * @author cbosdonnat
 *
 */
public class ClientWizard extends CCProjectWizard {

    private static final String SRC_DIR_NAME = "src"; //$NON-NLS-1$
    private static final String CLIENT_FILE = "client.cxx"; //$NON-NLS-1$
    
    private UnoConnectionPage mCnxPage;
    private IWorkbenchPage mActivePage;

    /**
     * Default constructor.
     */
    public ClientWizard() {
        super( );
        setWindowTitle( Messages.getString("ClientWizard.Title") ); //$NON-NLS-1$
        mActivePage = WorkbenchHelper.getActivePage();
    }
    
    @Override
    public void addPages() {
        mCnxPage = new UnoConnectionPage();
        UnoClientWizardPage mainPage = new UnoClientWizardPage( "cdtmain", mCnxPage ); //$NON-NLS-1$
        fMainPage = mainPage;
        fMainPage.setTitle( getWindowTitle() );
        fMainPage.setDescription( Messages.getString("ClientWizard.Description") ); //$NON-NLS-1$
        fMainPage.setImageDescriptor( Activator.imageDescriptorFromPlugin( Activator.PLUGIN_ID, 
                Messages.getString("ClientWizard.ClientWizardBanner") ) ); //$NON-NLS-1$
        
        addPage(fMainPage);
        
        mCnxPage.setMainPage( mainPage );
        addPage( mCnxPage );
        
    }
    
    @Override
    public boolean performFinish() {
        boolean finished = super.performFinish();
        
        try {
            IOOo ooo = mCnxPage.getOoo();
            ISdk sdk = mCnxPage.getSdk();

            // Copy the helper files in the helper source dir
            IFolder srcDir = newProject.getFolder( SRC_DIR_NAME );
            srcDir.create( true, true, null );

            copyResource( "connection.hxx", srcDir, new String() ); //$NON-NLS-1$
            copyResource( "connection.cxx", srcDir, new String() ); //$NON-NLS-1$
            
            String cnxInitCode = mCnxPage.getConnectionCode();
            copyResource( CLIENT_FILE, srcDir, cnxInitCode );

            srcDir.refreshLocal( IResource.DEPTH_ONE, null );

            // Add the helper dir to the source path entries
            ICProject cprj = CoreModel.getDefault().getCModel().getCProject( newProject.getName() );
            IPathEntry[] entries = CoreModel.getRawPathEntries( cprj );
            IPathEntry[] newEntries = new IPathEntry[ entries.length + 1 ];
            System.arraycopy( entries, 0, newEntries, 0, entries.length );
            newEntries[ newEntries.length - 1 ] = CoreModel.newSourceEntry( srcDir.getFullPath() );
            CoreModel.setRawPathEntries( cprj, newEntries, null );

            CppProjectHandler.addOOoDependencies( ooo, sdk, newProject );
            
            selectAndReveal( srcDir.getFile( CLIENT_FILE ) );
            WorkbenchHelper.showFile( srcDir.getFile( CLIENT_FILE ), mActivePage );
        
        } catch ( Exception e ) {
            PluginLogger.error( Messages.getString("ClientWizard.ClientConfigError"), e ); //$NON-NLS-1$
        }
        
        return finished;
    }

    /**
     * Copy a template resource into the generated project.
     * 
     * @param pResName the name of the file to copy
     * @param pSrcDir the folder where to put it
     * @param pReplacement the replacement value for the connection string
     */
    private void copyResource(String pResName, IContainer pSrcDir, String pReplacement ) {
        InputStream in = this.getClass().getResourceAsStream( pResName );
        File destFile = new File( pSrcDir.getLocation().toFile(), pResName );
        
        FileWriter out = null;
        try {
            
            LineNumberReader reader = new LineNumberReader( new InputStreamReader( in ) );
            out = new FileWriter( destFile );
            
            String line = reader.readLine();
            while ( line != null ) {
                out.append( MessageFormat.format( line, pReplacement ) + "\n" ); //$NON-NLS-1$
                line = reader.readLine();
            }
            
        } catch ( Exception e ) {
            
        } finally {
            try { in.close(); } catch ( Exception e ) { }
            try { out.close(); } catch ( Exception e ) { }
        }
    }
}