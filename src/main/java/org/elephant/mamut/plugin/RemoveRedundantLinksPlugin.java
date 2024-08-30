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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.mastodon.app.MastodonIcons;
import org.mastodon.app.ui.ViewMenuBuilder.MenuItem;
import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.MamutMenuBuilder;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

@Plugin( type = RemoveRedundantLinksPlugin.class )
public class RemoveRedundantLinksPlugin implements MamutPlugin
{

    private final static String ACTION_NAME = "remove redundant";

    private RemoveRedundantLinksAction action;

    @Override
    public void setAppPluginModel( final ProjectModel projectModel )
    {
        this.action = new RemoveRedundantLinksAction( projectModel );
    }

    @Override
    public void installGlobalActions( final Actions actions )
    {
        final String keyboardShortcut = "not mapped";

        actions.namedAction( action, keyboardShortcut );

    }

    private static class RemoveRedundantLinksAction extends AbstractNamedAction
    {

        private static final long serialVersionUID = 1L;

        private final ProjectModel projectModel;

        private final ModelGraph graph;

        final Spot vRef0;

        private class Pair
        {
            final int a;

            final int b;

            public Pair( final int a, final int b )
            {
                this.a = a;
                this.b = b;
            }

            @Override
            public boolean equals( Object o )
            {
                if ( o instanceof Pair )
                {
                    Pair p = ( Pair ) o;
                    return p.a == this.a && p.b == this.b;
                }
                return false;
            }

            @Override
            public int hashCode()
            {
                return Objects.hash( a, b );
            }
        }

        private RemoveRedundantLinksAction( final ProjectModel projectModel )
        {
            super( ACTION_NAME );
            this.projectModel = projectModel;
            this.graph = projectModel.getModel().getGraph();
            this.vRef0 = graph.vertexRef();
        }

        @Override
        public void actionPerformed( final ActionEvent e )
        {
            final RefCollection< Link > linksToRemove = RefCollections.createRefSet( graph.edges() );
            final Set< Pair > sourceTargetSet = new HashSet<>();
            graph.getLock().readLock().lock();
            try
            {
                for ( final Link link : graph.edges() )
                {
                    final int sourceId = link.getSource( vRef0 ).getInternalPoolIndex();
                    final int targetId = link.getTarget( vRef0 ).getInternalPoolIndex();
                    final Pair pair = new Pair( sourceId, targetId );
                    if ( sourceTargetSet.contains( pair ) )
                    {
                        System.out.println( String.format( "link %s is duplicated", link ) );
                        linksToRemove.add( link );
                    }
                    sourceTargetSet.add( pair );
                }
            }
            finally
            {
                graph.getLock().readLock().unlock();
                graph.releaseRef( vRef0 );
            }

            // Remove redundant links.
            graph.getLock().writeLock().lock();
            try
            {
                for ( final Link link : linksToRemove )
                {
                    graph.remove( link );
                }
            }
            finally
            {
                projectModel.getModel().setUndoPoint();
                graph.getLock().writeLock().unlock();
                if ( EventQueue.isDispatchThread() )
                {
                    graph.notifyGraphChanged();
                }
                else
                {
                    SwingUtilities.invokeLater( () -> graph.notifyGraphChanged() );
                }
            }

            /*
             * Let's show this to the user.
             */
            final Date now = new Date();
            final String dateTxt = new SimpleDateFormat( "YYYY-MM-dd HH:MM" ).format( now );
            final String message = "On " + dateTxt + ", there were " + linksToRemove.size() + " redundant links.";
            JOptionPane.showMessageDialog( null,
                    message,
                    "example Mastodon plugin",
                    JOptionPane.INFORMATION_MESSAGE,
                    MastodonIcons.MASTODON_ICON_MEDIUM );
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
            final String description = "Remove redundant links.";
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
                "Remove redundant links" );
    }
}
