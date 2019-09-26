package gr.sqlbrowserfx.nodes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import gr.sqlbrowserfx.utils.AutoComplete;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.SyntaxUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class SqlCodeArea extends CodeArea {

    private static final int LIST_ITEM_HEIGHT = 30;
    private static final int LIST_MAX_HEIGHT = 120;
    
	private Runnable enterAction;
	private boolean auoCompletePopupShowing = false;
	private AtomicReference<Popup> auoCompletePopup;

	public SqlCodeArea() {
		super();
		this.setContextMenu(this.createContextMenu());
		auoCompletePopup = new AtomicReference<Popup>();
		this.setOnKeyTyped(keyEvent -> this.autoCompleteAction(keyEvent, auoCompletePopup));

		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.ENTER) {
				enterAction.run();
			} else if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
				if (auoCompletePopupShowing) {
					auoCompletePopup.get().hide();
					auoCompletePopup.set(null);
					auoCompletePopupShowing = false;
				}
				// uncomment this to activate autocomplete on backspace
//				this.autoCompleteAction(keyEvent, auoCompletePopup);
			} else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.Q) {
				// TODO go to query x tab
			}
		});

		this.setOnMouseClicked(mouseEvent -> {
			if (auoCompletePopupShowing) {
				auoCompletePopup.get().hide();
				auoCompletePopupShowing = false;
			}
		});
//FIXME no need for this listener to be removed
//		this.caretPositionProperty().addListener((observable, oldPosition, newPosition) -> {
//			if (auoCompletePopup.get() != null) {
//				auoCompletePopup.get().hide();
//				auoCompletePopupShowing = false;
//			}
//		});

		// Unsubscribe when not needed
		@SuppressWarnings("unused")
		Subscription subscription = this.multiPlainChanges().successionEnds(Duration.ofMillis(100))
				.subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));

	}

	private ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());

		MenuItem menuItemCut = new MenuItem("Cut", JavaFXUtils.icon("/res/cut.png"));
		menuItemCut.setOnAction(event -> this.cut());

		MenuItem menuItemPaste = new MenuItem("Paste", JavaFXUtils.icon("/res/paste.png"));
		menuItemPaste.setOnAction(event -> this.paste());

		MenuItem menuItemSuggestions = new MenuItem("Suggestions");// , JavaFXUtils.icon("/res/paste.png"));
		menuItemSuggestions
				.setOnAction(event -> this.autoCompleteAction(this.simulateControlSpaceEvent(), auoCompletePopup));

		menu.getItems().addAll(menuItemCopy, menuItemCut, menuItemPaste, menuItemSuggestions);
		return menu;
	}

	private KeyEvent simulateControlSpaceEvent() {
		return new KeyEvent(KeyEvent.KEY_TYPED, " ", " ", null, false, true, false, false);
	}

	private ListView<String> createListView(List<String> suggestions){
        ListView<String> suggestionsList = new ListView<>();
        suggestionsList.getItems().addAll(FXCollections.observableList(new ArrayList<>(new HashSet<>(suggestions))));
        int suggestionsNum = suggestions.size();
        int listViewLength = ((suggestionsNum * LIST_ITEM_HEIGHT) > LIST_MAX_HEIGHT) ? LIST_MAX_HEIGHT : suggestionsNum * LIST_ITEM_HEIGHT;
        suggestionsList.setPrefHeight(listViewLength);
        return suggestionsList;
    }
	
	private void autoCompleteAction(KeyEvent event, AtomicReference<Popup> auoCompletePopup) {
		String ch = event.getCharacter();
		// for some reason keycode does not work
		if (Character.isLetter(ch.charAt(0)) || (event.isControlDown() && ch.equals(" "))
				|| event.getCode() == KeyCode.BACK_SPACE) {
			int position = this.getCaretPosition();
			String query = AutoComplete.getQuery(this, position);
			if (auoCompletePopup.get() == null) {
				Popup popup = new Popup();
				popup.setAutoHide(true);
				popup.setOnAutoHide(event2 -> auoCompletePopupShowing = false);
				auoCompletePopup.set(popup);
			}

			if (!query.trim().isEmpty()) {
				ListView<String> suggestionsList = this.createListView(AutoComplete.getQuerySuggestions(query));
				if (suggestionsList.getItems().size() != 0) {
					auoCompletePopup.get().getContent().setAll(suggestionsList);
					Bounds pointer = this.caretBoundsProperty().getValue().get();
					if (!auoCompletePopupShowing) {
						auoCompletePopup.get().show(this, pointer.getMaxX(), pointer.getMinY() + 20);
						auoCompletePopupShowing = true;
					}
					suggestionsList.setOnKeyPressed(keyEvent -> {
						if (keyEvent.getCode() == KeyCode.ENTER) {
							AtomicReference<String> word = new AtomicReference<>();
							if (suggestionsList.getSelectionModel().getSelectedItem() != null) {
								word.set(suggestionsList.getSelectionModel().getSelectedItem().toString());
							} else {
								word.set(suggestionsList.getItems().get(0).toString());
							}
							Platform.runLater(() -> {
								this.replaceText(position - query.length(), position, word.get());
								this.moveTo(position + word.get().length() - query.length());
							});
							auoCompletePopup.get().hide();
							auoCompletePopupShowing = false;
						}
						if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.SPACE) {
							auoCompletePopup.get().hide();
							auoCompletePopupShowing = false;
						}
					});
				} else {
					auoCompletePopup.get().hide();
					auoCompletePopupShowing = false;
				}
			} else {
				auoCompletePopup.get().hide();
				auoCompletePopupShowing = false;
			}
		} else {
			auoCompletePopup.get().hide();
			auoCompletePopupShowing = false;
		}
	}

	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = SyntaxUtils.PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = matcher.group("KEYWORD") != null ? "keyword"
					: matcher.group("FUNCTION") != null ? "function"
							: matcher.group("METHOD") != null ? "method" : matcher.group("PAREN") != null ? "paren"
//							: matcher.group("BRACE") != null ? "brace"
//									: matcher.group("BRACKET") != null ? "bracket"
									: matcher.group("SEMICOLON") != null ? "semicolon"
											: matcher.group("STRING2") != null ? "string2"
													: matcher.group("STRING") != null ? "string" : null;
//															: matcher.group("COMMENT") != null ? "comment" : null;
			/* never happens */ assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	public void setEnterAction(Runnable action) {
		enterAction = action;
	}
}
