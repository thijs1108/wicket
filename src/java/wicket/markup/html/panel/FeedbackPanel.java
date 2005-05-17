/*
 * $Id$
 * $Revision$ $Date$
 * 
 * ==============================================================================
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
package wicket.markup.html.panel;

import wicket.AttributeModifier;
import wicket.Component;
import wicket.FeedbackMessage;
import wicket.FeedbackMessagesModel;
import wicket.IFeedback;
import wicket.IFeedbackBoundary;
import wicket.markup.html.basic.Label;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;
import wicket.model.AbstractModel;
import wicket.model.IModel;

/**
 * A simple panel that displays {@link wicket.FeedbackMessage}s in a list view.
 * The maximum number of messages to show can be set with setMaxMessages().
 * 
 * @see wicket.FeedbackMessage
 * @see wicket.FeedbackMessages
 * @author Jonathan Locke
 * @author Eelco Hillenius
 */
public class FeedbackPanel extends Panel implements IFeedback
{
	/** Message view */
	private final MessageListView messageListView;

	/**
	 * Optional collecting component. When this is not set explicitly, the first occurence
	 * of {@link IFeedbackBoundary} will be searched for higher up in the run-time
	 * hierarchy.
	 */

	/**
	 * List for messages.
	 */
	private static final class MessageListView extends ListView
	{
		/**
		 * @see wicket.Component#Component(String)
		 */
		public MessageListView(final String id)
		{
			super(id, new FeedbackMessagesModel(true, null));
		}

		/**
		 * @see wicket.markup.html.list.ListView#populateItem(wicket.markup.html.list.ListItem)
		 */
		protected void populateItem(final ListItem listItem)
		{
			final FeedbackMessage message = (FeedbackMessage)listItem.getModelObject();
			IModel replacementModel = new AbstractModel()
			{
				/**
				 * Returns feedbackPanel + the message level, eg
				 * 'feedbackPanelERROR'. This is used as the class of the li /
				 * span elements.
				 * 
				 * @see wicket.model.IModel#getObject(Component)
				 */
				public Object getObject(final Component component)
				{
					return "feedbackPanel" + message.getLevelAsString();
				}

				/**
				 * @see wicket.model.IModel#setObject(Component, Object)
				 */
				public void setObject(final Component component, final Object object)
				{
				}

				public IModel getNestedModel()
				{
					// TODO check calls to getNestedModel(), returned message
					return null;
				}
			};

			final Label label = new Label("message", message.getMessage());
			final AttributeModifier levelModifier = new AttributeModifier("class", replacementModel);
			label.add(levelModifier);
			listItem.add(levelModifier);
			listItem.add(label);
		}
	}

	/**
	 * @see wicket.Component#Component(String)
	 */
	public FeedbackPanel(final String id)
	{
		super(id);
		this.messageListView = new MessageListView("messages");
		messageListView.setVersioned(false);
		add(messageListView);
	}

	/**
	 * @param maxMessages
	 *            The maximum number of feedback messages that this feedback
	 *            panel should show at one time
	 */
	public void setMaxMessages(int maxMessages)
	{
		this.messageListView.setViewSize(maxMessages);
	}

	/**
	 * Sets the optional collecting component. When this is not set explicitly, the first occurence
	 * of {@link IFeedbackBoundary} will be searched for higher up in the run-time
	 * hierarchy.
	 * @param collectingComponent the collecting component
	 */
	public void setCollectingComponent(Component collectingComponent)
	{
		FeedbackMessagesModel feedbackMessagesModel =
			(FeedbackMessagesModel)messageListView.getModel();
		feedbackMessagesModel.setCollectingComponent(collectingComponent);
	}
}
