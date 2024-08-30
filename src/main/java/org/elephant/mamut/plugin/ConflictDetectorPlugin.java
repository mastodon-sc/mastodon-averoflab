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

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mastodon.app.ui.ViewMenuBuilder.MenuItem;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.MamutMenuBuilder;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.model.tag.ObjTagMap;
import org.mastodon.model.tag.TagSetStructure.Tag;
import org.mastodon.model.tag.TagSetStructure.TagSet;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

@Plugin( type = ConflictDetectorPlugin.class )
public class ConflictDetectorPlugin implements MamutPlugin
{

    private final static String ACTION_NAME = "conflict detector";

    private ConflictDetectorAction action;

    @Override
    public void setAppPluginModel( final ProjectModel projectModel )
    {
        this.action = new ConflictDetectorAction( projectModel );
    }

    @Override
    public void installGlobalActions( final Actions actions )
    {
        final String keyboardShortcut = "not mapped";

        actions.namedAction( action, keyboardShortcut );

    }

    private static class ConflictDetectorAction extends AbstractNamedAction
    {

        private static final long serialVersionUID = 1L;

        private final ProjectModel projectModel;

        private final ModelGraph graph;

        final double[] pos = new double[ 3 ];

        private ConflictDetectorAction( final ProjectModel projectModel )
        {
            super( ACTION_NAME );
            this.projectModel = projectModel;
            this.graph = projectModel.getModel().getGraph();
        }

        @Override
        public void actionPerformed( final ActionEvent e )
        {
            TagSet tagSet = projectModel.getModel().getTagSetModel().getTagSetStructure().getTagSets().stream()
                    .filter( ts -> ts.getName().equals( "Duplicate" ) ).findFirst().orElse( null );
            Tag tag = tagSet.getTags().stream().filter( t -> t.label().equals( "duplicate" ) ).findFirst().orElse( null );
            ObjTagMap< Spot, Tag > tagMap = projectModel.getModel().getTagSetModel().getVertexTags().tags( tagSet );
            for ( int t = projectModel.getMinTimepoint(); t <= projectModel.getMaxTimepoint(); t++ )
            {
                final int time = t;
                Set< String > hashSet = new HashSet<>();
                graph.vertices().stream().filter( spot -> spot.getTimepoint() == time ).forEach( spot -> {
                    spot.localize( pos );
                    String hash = GeoHash3D.geoHashStringWithCharacterPrecision( pos[ 0 ], pos[ 1 ], pos[ 2 ], 4 );
                    if ( hashSet.contains( hash ) )
                    {
                        tagMap.set( spot, tag );
                        System.out.println( "Conflict: " + spot );
                    }
                    hashSet.add( hash );
                } );
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
            final String description = "Detect conflicts.";
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
                "Detect conflicts" );
    }
}
