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
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.mastodon.app.MastodonIcons;
import org.mastodon.app.ui.ViewMenuBuilder.MenuItem;
import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefDeque;
import org.mastodon.graph.ref.OutgoingEdges;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.MamutMenuBuilder;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.model.tag.TagSetStructure.Tag;
import org.mastodon.model.tag.TagSetStructure.TagSet;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

@Plugin( type = ReduceTPlugin.class )
public class ReduceTPlugin implements MamutPlugin
{

    private final static String ACTION_NAME = "reduce t";

    private ReduceTAction action;

    @Override
    public void setAppPluginModel( final ProjectModel projectModel )
    {
        this.action = new ReduceTAction( projectModel );
    }

    @Override
    public void installGlobalActions( final Actions actions )
    {
        final String keyboardShortcut = "not mapped";

        actions.namedAction( action, keyboardShortcut );

    }

    private static class ReduceTAction extends AbstractNamedAction
    {

        private static final long serialVersionUID = 1L;

        private final ProjectModel projectModel;

        private final ModelGraph graph;

        final double[] pos = new double[ 3 ];

        final double[][] cov = new double[ 3 ][ 3 ];

        final Spot vRef0;

        final Spot vRef1;

        final Spot vRef2;

        final Spot vRef3;

        final Link eRef0;

        final Link eRef1;

        final Link eRef2;

        final List< TagSet > tagSets;

        private ReduceTAction( final ProjectModel projectModel )
        {
            super( ACTION_NAME );
            this.projectModel = projectModel;
            this.graph = projectModel.getModel().getGraph();
            this.vRef0 = graph.vertexRef();
            this.vRef1 = graph.vertexRef();
            this.vRef2 = graph.vertexRef();
            this.vRef3 = graph.vertexRef();
            this.eRef0 = graph.edgeRef();
            this.eRef1 = graph.edgeRef();
            this.eRef2 = graph.edgeRef();
            tagSets = projectModel.getModel().getTagSetModel().getTagSetStructure().getTagSets();
        }

        private void processSpot( final Spot spot, final Spot copiedSpot,
                final RefDeque< Spot > spotDeque, final RefDeque< Link > linkDeque1, final RefDeque< Link > linkDeque2 )
        {
            if ( spot.getTimepoint() % 2 == 1 )
            {
                final OutgoingEdges< Link > outgoingEdges0 = spot.outgoingEdges();
                for ( final Link link : outgoingEdges0 )
                {
                    link.getTarget( vRef0 );
                    processSpot( vRef0, null, spotDeque, linkDeque1, linkDeque2 );
                }
            }
            else
            {
                if ( copiedSpot == null )
                {
                    spot.localize( pos );
                    spot.getCovariance( cov );
                    graph.addVertex( vRef1 ).init( spot.getTimepoint() / 2, pos, cov );
                    for ( final TagSet tagSet : tagSets )
                    {
                        final Tag tag = projectModel.getModel().getTagSetModel().getVertexTags().tags( tagSet ).get( spot );
                        projectModel.getModel().getTagSetModel().getVertexTags().tags( tagSet ).set( vRef1, tag );
                    }
                }
                else
                {
                    vRef1.refTo( copiedSpot );
                }
                for ( final Link link1 : spot.outgoingEdges() )
                {
                    spotDeque.push( vRef1 );
                    linkDeque1.push( link1 );
                }
                while ( 0 < linkDeque1.size() )
                {
                    linkDeque1.pop( eRef1 );
                    spotDeque.pop( vRef1 );

                    eRef1.getTarget( vRef2 );

                    for ( final Link link1 : vRef2.outgoingEdges() )
                    {
                        spotDeque.push( vRef1 );
                        linkDeque2.push( link1 );
                    }
                    while ( 0 < linkDeque2.size() )
                    {
                        linkDeque2.pop( eRef2 );
                        spotDeque.pop( vRef1 );
                        eRef2.getTarget( vRef2 );
                        vRef2.localize( pos );
                        vRef2.getCovariance( cov );
                        graph.addVertex( vRef3 ).init( vRef2.getTimepoint() / 2, pos, cov );
                        graph.addEdge( vRef1, vRef3, eRef0 ).init();
                        for ( final TagSet tagSet : tagSets )
                        {
                            final Tag vTag = projectModel.getModel().getTagSetModel().getVertexTags().tags( tagSet ).get( vRef2 );
                            projectModel.getModel().getTagSetModel().getVertexTags().tags( tagSet ).set( vRef3, vTag );
                            final Tag eTag = projectModel.getModel().getTagSetModel().getEdgeTags().tags( tagSet ).get( eRef2 );
                            projectModel.getModel().getTagSetModel().getEdgeTags().tags( tagSet ).set( eRef0, eTag );
                        }
                        processSpot( vRef2, vRef3, spotDeque, linkDeque1, linkDeque2 );
                    }
                }
            }
        }

        @Override
        public void actionPerformed( final ActionEvent e )
        {
            final RefCollection< Spot > spots = RefCollections.createRefSet( graph.vertices() );
            final RefCollection< Spot > rootSpots = RefCollections.createRefSet( graph.vertices() );
            final RefDeque< Spot > spotDeque = RefCollections.createRefDeque( graph.vertices() );
            final RefDeque< Link > linkDeque1 = RefCollections.createRefDeque( graph.edges() );
            final RefDeque< Link > linkDeque2 = RefCollections.createRefDeque( graph.edges() );
            graph.getLock().readLock().lock();
            try
            {
                spots.addAll( graph.vertices() );
                for ( final Spot spot : spots )
                {
                    if ( spot.incomingEdges().isEmpty() )
                        rootSpots.add( spot );
                }
            }
            finally
            {
                graph.getLock().readLock().unlock();
            }

            // Keep previous spots to remove later.

            graph.getLock().writeLock().lock();
            try
            {
                for ( final Spot rootSpot : rootSpots )
                {
                    processSpot( rootSpot, null, spotDeque, linkDeque1, linkDeque2 );
                }
            }
            finally
            {
                graph.getLock().writeLock().unlock();
                graph.releaseRef( vRef0 );
                graph.releaseRef( vRef1 );
                graph.releaseRef( vRef2 );
                graph.releaseRef( vRef3 );
                graph.releaseRef( eRef0 );
                graph.releaseRef( eRef1 );
                graph.releaseRef( eRef2 );
            }

            // Remove previous spots.
            graph.getLock().writeLock().lock();
            try
            {
                for ( final Spot spot : spots )
                {
                    graph.remove( spot );
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
             * The graph has vertices, which are the spots, or cells, in our
             * data.
             */
            final int nSpots = graph.vertices().size();

            /*
             * Let's show this to the user.
             */
            final Date now = new Date();
            final String dateTxt = new SimpleDateFormat( "YYYY-MM-dd HH:MM" ).format( now );
            final String message = "On " + dateTxt + ", there were " + nSpots + " spots.";
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
            final String description = "Reduce timepoints with a specified reduce factor.";
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
                "Reduce timepoint" );
    }
}
