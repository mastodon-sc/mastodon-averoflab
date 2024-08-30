/*******************************************************************************
 * Copyright (C) 2024, Ko Sugawara
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.elephant.mamut.plugin;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.elephant.mamut.plugin.swing.TranslateDialog;
import org.mastodon.app.ui.ViewMenuBuilder.MenuItem;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.MamutMenuBuilder;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

@Plugin( type = TranslatePlugin.class )
public class TranslatePlugin implements MamutPlugin
{

    private final static String ACTION_NAME = "translate";

    private TranslateAction action;

    @Override
    public void setAppPluginModel( final ProjectModel projectModel )
    {
        this.action = new TranslateAction( projectModel );
    }

    @Override
    public void installGlobalActions( final Actions actions )
    {
        final String keyboardShortcut = "not mapped";

        actions.namedAction( action, keyboardShortcut );

    }

    private static class TranslateAction extends AbstractNamedAction
    {

        private static final long serialVersionUID = 1L;

        private final ProjectModel projectModel;

        private final ModelGraph graph;

        final double[] pos = new double[ 3 ];

        final Spot vRef0;

        private TranslateAction( final ProjectModel projectModel )
        {
            super( ACTION_NAME );
            this.projectModel = projectModel;
            this.graph = projectModel.getModel().getGraph();
            this.vRef0 = graph.vertexRef();
        }

        @Override
        public void actionPerformed( final ActionEvent e )
        {
            final TranslateDialog dialog = new TranslateDialog();
            dialog.setVisible( true );

            if ( dialog.isCanceled() )
                return;
            final double translateX = dialog.getTranslateX();
            final double translateY = dialog.getTranslateY();
            final double translateZ = dialog.getTranslateZ();

            graph.getLock().writeLock().lock();
            try
            {
                for ( final Spot spot : graph.vertices() )
                {
                    vRef0.refTo( spot );
                    vRef0.localize( pos );
                    pos[ 0 ] += translateX;
                    pos[ 1 ] += translateY;
                    pos[ 2 ] += translateZ;
                    vRef0.setPosition( pos );
                }
            }
            finally
            {
                projectModel.getModel().setUndoPoint();
                graph.getLock().writeLock().unlock();
                graph.releaseRef( vRef0 );
                if ( EventQueue.isDispatchThread() )
                {
                    graph.notifyGraphChanged();
                }
                else
                {
                    SwingUtilities.invokeLater( () -> graph.notifyGraphChanged() );
                }
            }
        }
    }

    @Plugin( type = Descriptions.class )
    public static class Descriptions extends CommandDescriptionProvider
    {
        public Descriptions()
        {
            super( KeyConfigScopes.MAMUT, KeyConfigContexts.MASTODON );
        }

        @Override
        public void getCommandDescriptions( final CommandDescriptions descriptions )
        {
            final String actionName = ACTION_NAME;
            final String[] keyboardShortcut = new String[] { "not mapped" };
            final String description = "Translate spots.";
            descriptions.add( actionName, keyboardShortcut, description );
        }

    }

    @Override
    public List< MenuItem > getMenuItems()
    {
        final String actionName = ACTION_NAME;
        final MenuItem menuItem = MamutMenuBuilder.makeFullMenuItem(
                actionName,
                "Plugins", "Averof Lab" );
        return Collections.singletonList( menuItem );
    }

    @Override
    public Map< String, String > getMenuTexts()
    {
        final String actionName = ACTION_NAME;
        return Collections.singletonMap(
                actionName,
                "Translate" );
    }

}
