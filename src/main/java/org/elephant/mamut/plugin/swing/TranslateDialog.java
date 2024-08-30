/*******************************************************************************
 * Copyright (C) 2020, Ko Sugawara
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
package org.elephant.mamut.plugin.swing;

import javax.swing.*;
import javax.swing.SpinnerNumberModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TranslateDialog extends JDialog
{
    private JSpinner spinnerX;

    private JSpinner spinnerY;

    private JSpinner spinnerZ;

    private boolean isCanceled = true;

    private JButton btnOk;

    private JButton btnCancel;

    private double translateX;

    private double translateY;

    private double translateZ;

    public TranslateDialog()
    {
        setModal( true );
        setLayout( new GridLayout( 4, 2 ) );

        spinnerX = createDoubleSpinner();
        spinnerY = createDoubleSpinner();
        spinnerZ = createDoubleSpinner();

        btnOk = new JButton( "OK" );
        btnOk.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                if ( validateInputs() )
                {
                    translateX = ( double ) spinnerX.getValue();
                    translateY = ( double ) spinnerY.getValue();
                    translateZ = ( double ) spinnerZ.getValue();
                    isCanceled = false;
                    setVisible( false );
                }
                else
                {
                    JOptionPane.showMessageDialog( null, "Invalid input. Please enter valid double values." );
                }
            }
        } );

        final JButton btnCancel = new JButton( "Cancel" );
        btnCancel.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                isCanceled = true;
                setVisible( false );
            }
        } );

        add( new JLabel( "Translate X:" ) );
        add( spinnerX );
        add( new JLabel( "Translate Y:" ) );
        add( spinnerY );
        add( new JLabel( "Translate Z:" ) );
        add( spinnerZ );

        JPanel btnPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        btnPanel.add( btnOk );
        btnPanel.add( btnCancel );

        add( btnPanel );
    }

    private JSpinner createDoubleSpinner()
    {
        return new JSpinner( new SpinnerNumberModel( 0.0, -Double.MAX_VALUE, Double.MAX_VALUE, 0.1 ) );
    }

    private boolean validateInputs()
    {
        return validateSpinner( spinnerX ) && validateSpinner( spinnerY ) && validateSpinner( spinnerZ );
    }

    private boolean validateSpinner( JSpinner spinner )
    {
        try
        {
            spinner.commitEdit();
            return true;
        }
        catch ( java.text.ParseException e )
        {
            return false;
        }
    }

    public double getTranslateX()
    {
        return translateX;
    }

    public double getTranslateY()
    {
        return translateY;
    }

    public double getTranslateZ()
    {
        return translateZ;
    }

    public boolean isCanceled()
    {
        return isCanceled;
    }
}
