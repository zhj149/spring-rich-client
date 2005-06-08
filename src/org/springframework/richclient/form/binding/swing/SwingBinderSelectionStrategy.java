/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.richclient.form.binding.swing;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.springframework.core.enums.LabeledEnum;
import org.springframework.richclient.form.binding.support.AbstractBinderSelectionStrategy;

/**
 * @author Oliver Hutchison
 */
public class SwingBinderSelectionStrategy extends AbstractBinderSelectionStrategy {

    public SwingBinderSelectionStrategy() {
        super(JTextField.class);
    }

    protected void registerDefaultBinders() {        
        registerBinderForPropertyType(String.class, new TextComponentBinder());
        registerBinderForPropertyType(boolean.class, new CheckBoxBinder());
        registerBinderForPropertyType(Boolean.class, new CheckBoxBinder());
        registerBinderForPropertyType(LabeledEnum.class, new EnumComboBoxBinder());
        registerBinderForControlType(JTextComponent.class, new TextComponentBinder());
        registerBinderForControlType(JFormattedTextField.class, new FormattedTextFieldBinder(null));
        registerBinderForControlType(JTextArea.class, new TextAreaBinder());
        registerBinderForControlType(JCheckBox.class, new CheckBoxBinder());
        registerBinderForControlType(JComboBox.class, new ComboBoxBinder());
    }
}