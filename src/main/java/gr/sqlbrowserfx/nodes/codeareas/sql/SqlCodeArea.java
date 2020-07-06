package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class SqlCodeArea extends CodeArea implements SimpleChangeListener<String>, ContextMenuOwner, HighLighter {

	private static final int LIST_ITEM_HEIGHT = 30;
	private static final int LIST_MAX_HEIGHT = 120;

	private Runnable enterAction;
	private boolean autoCompletePopupShowing = false;
	private Popup autoCompletePopup;
	private boolean autoCompleteOnType = true;
	protected SearchAndReplacePopOver searchAndReplacePopOver;
	private String saveTableShortcut;
	private ListView<String> suggestionsList;
	private AtomicReference<String> word = new AtomicReference<>();
	public final HashMap<String, Set<String>> tableAliases = new HashMap<>();

	public SqlCodeArea() {
		this(null);
	}
	
	public SqlCodeArea(String text) {
		autoCompletePopup = new Popup();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this);

		this.setOnKeyTyped(keyEvent -> this.autoCompleteAction(keyEvent));
		this.setContextMenu(this.createContextMenu());
		this.setKeys();

		this.setOnMouseClicked(mouseEvent -> {
			if (autoCompletePopupShowing) {
				hideAutocompletePopup();
			}
			searchAndReplacePopOver.hide();
		});

		this.enableHighlighting();
		if (this.isEditable())
			this.startTextAnalyzerDaemon();
	}

	private void startTextAnalyzerDaemon() {
		Thread th = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(10*60*1000);
					//TODO Create a new map an d whenready then switch the pointer
					SqlCodeArea.this.tableAliases.clear();
					SqlCodeArea.this.analyzeTextForTablesAliases(this.getText());
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		}, "Text Analyzer Daemon");
		th.setDaemon(true);
		th.start();
	}


	@Override
	public void appendText(String text) {
		super.appendText(text);
		System.out.println("appending text");
		this.analyzeTextForTablesAliases(text);
	}
	
	private void setKeys() {
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) { 
				if (keyEvent.getCode() == KeyCode.ENTER) {
					enterAction.run();
				}
				else if (keyEvent.getCode() == KeyCode.SPACE) {
					this.autoCompleteAction(keyEvent);
				}
				else if (keyEvent.getCode() == KeyCode.Q) {
					// TODO go to query x tab
				}
				else if (keyEvent.getCode() == KeyCode.F) {
					this.showSearchAndReplacePopup();
				}
				else if (keyEvent.getCode() == KeyCode.D) {
					boolean hasInitialSelectedText = false;
					if (this.getSelectedText().isEmpty())
						this.selectLine();
					else
						hasInitialSelectedText = true;
					
					this.replaceSelection("");
					
					if (!hasInitialSelectedText && this.getCaretPosition() != 0) {
						this.deletePreviousChar();
						this.moveTo(this.getCaretPosition());
					}
				}
			}
			else {
				if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
					this.hideAutocompletePopup();
					// uncomment this to activate autocomplete on backspace
//					this.autoCompleteAction(keyEvent, auoCompletePopup);
				}
				else if (keyEvent.getCode() == KeyCode.ENTER) {
					this.autoCompleteAction(keyEvent);
				}
			}
		});
	}
	
	private int countLines(String str) {
		String[] lines = str.split("\r\n|\r|\n");
		return lines.length;
	}
	
	public SqlCodeArea(String text, boolean editable, boolean withMenu) {
		super();

		if (!withMenu)
			this.setContextMenu(null);
		
		this.enableHighlighting();
		if (text != null) {
			this.replaceText(text);
			this.setPrefHeight(countLines(text)*18);
		}
		
		this.setEditable(editable);
		

		this.setOnMouseClicked(mouseEvent -> {
			this.requestFocus();
			if (mouseEvent.getClickCount() == 2) {
				this.selectAll();
			}
		});
		this.focusedProperty().addListener((ov, oldV, newV) -> {
			if (!newV) { // focus lost
				this.deselect();
			}
		});
	}

	@Override
	public void enableHighlighting() {
		@SuppressWarnings("unused")
		Subscription subscription = this.multiPlainChanges().successionEnds(Duration.ofMillis(100))
				.subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));
	}
	
	protected void showSearchAndReplacePopup() {
		if (!this.getSelectedText().isEmpty()) {
			searchAndReplacePopOver.getFindField().setText(this.getSelectedText());
			searchAndReplacePopOver.getFindField().selectAll();
		}
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		searchAndReplacePopOver.show(this, boundsInScene.getMaxX() - searchAndReplacePopOver.getWidth(),
				boundsInScene.getMinY());
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());

		MenuItem menuItemCut = new MenuItem("Cut", JavaFXUtils.icon("/res/cut.png"));
		menuItemCut.setOnAction(event -> this.cut());

		MenuItem menuItemPaste = new MenuItem("Paste", JavaFXUtils.icon("/res/paste.png"));
		menuItemPaste.setOnAction(event -> this.paste());

		MenuItem menuItemSuggestions = new MenuItem("Suggestions", JavaFXUtils.icon("/res/suggestion.png"));
		menuItemSuggestions
				.setOnAction(event -> this.autoCompleteAction(this.simulateControlSpaceEvent()));

		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.icon("/res/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		menu.getItems().addAll(menuItemCopy, menuItemCut, menuItemPaste, menuItemSuggestions, menuItemSearchAndReplace);
		return menu;
	}

	private KeyEvent simulateControlSpaceEvent() {
		return new KeyEvent(KeyEvent.KEY_TYPED, " ", " ", null, false, true, false, false);
	}

	private ListView<String> createListView(List<String> suggestions) {
		ListView<String> suggestionsList = new ListView<>();
		if (suggestions != null) {
			suggestionsList.getItems().addAll(FXCollections.observableList(new ArrayList<>(new HashSet<>(suggestions))));
			int suggestionsNum = suggestions.size();
			int listViewLength = ((suggestionsNum * LIST_ITEM_HEIGHT) > LIST_MAX_HEIGHT) ? LIST_MAX_HEIGHT
					: suggestionsNum * LIST_ITEM_HEIGHT;
			suggestionsList.setPrefHeight(listViewLength);
		}
		return suggestionsList;
	}

	private void autoCompleteAction(KeyEvent event) {
		
		boolean insertMode = false;
		String ch = event.getCharacter();
		if ((Character.isLetter(ch.charAt(0)) && autoCompleteOnType && !event.isControlDown())
				|| (event.isControlDown() && event.getCode() == KeyCode.SPACE)
				|| ch.equals(".") || ch.equals(",") || ch.equals("_")
				|| (ch.equals(" ") && saveTableShortcut != null)
				|| event.getCode() == KeyCode.ENTER
				|| event.getCode() == KeyCode.BACK_SPACE) {

			int caretPosition = this.getCaretPosition();
			String query = this.getQuery(caretPosition);
//			final String fQuery = query;
//			if (autoCompletePopupShowing) {
//				suggestionsList.setItems(
//					FXCollections.observableArrayList(suggestionsList
//								 .getItems()
//						         .stream()
//						         .filter(e -> e.contains(fQuery))
//						         .collect(Collectors.toList())
//				));
//				return;
//			}
			
			if (autoCompletePopup == null)
				autoCompletePopup = this.createPopup();

			if (!query.trim().isEmpty()) {
				List<String> suggestions = null;
				if (ch.equals(".")) {
					insertMode = true;
					query = this.calculateQuery(ch, caretPosition);
					suggestions = this.getColumnsSuggestions(query);
				}
				else if ( (ch.equals(",") || ch.equals(" ") || event.getCode() == KeyCode.ENTER) && saveTableShortcut != null) {
					query = this.calculateQuery(ch, caretPosition);
					System.out.println("Proccessing " + query);
					if (!query.isEmpty() && !query.equals(saveTableShortcut.trim()) && !query.equals("as")) {
						this.cacheTableAlias(query, saveTableShortcut);
						saveTableShortcut = null;
						return;
					}
				}
				else if (event.getCode() == KeyCode.ENTER) {
					if (word.get() != null) {
						query = word.get();
						System.out.println("'" + query + "'");
						if (SqlCodeAreaSyntax.COLUMNS_MAP.containsKey(query)) {
							System.out.println("Detected table " + query);
							if (!tableAliases.containsKey(query))
								tableAliases.put(query, new HashSet<>());
							saveTableShortcut = query;
						}
						word.set(null);
					}
					return;
				}
				else {
					if (SqlCodeAreaSyntax.COLUMNS_MAP.containsKey(query)) {
						System.out.println("Detected table " + query);
						if (!tableAliases.containsKey(query))
							tableAliases.put(query, new HashSet<>());
						saveTableShortcut = query;
					}
					suggestions = this.getQuerySuggestions(query);
				}
				
				suggestionsList = this.createListView(suggestions);
				if (suggestionsList.getItems().size() != 0) {
					autoCompletePopup.getContent().setAll(suggestionsList);
					this.showAutoCompletePopup();
					this.setOnSuggestionListKeyPressed(suggestionsList, insertMode, query, caretPosition);
				} else {
					this.hideAutocompletePopup();
				}
				
			} else {
				this.hideAutocompletePopup();
			}
		} else if (!event.isControlDown()) {
				this.hideAutocompletePopup();
		}
		
		event.consume();
	}

	private void cacheTableAlias(final String query, final String table) {
		for ( Collection<String> l : tableAliases.values()) {
			l.remove(query);
			break;
		}
		tableAliases.get(table).add(query);
	}

	private void hideAutocompletePopup() {
		if (autoCompletePopup != null && autoCompletePopupShowing) {
			autoCompletePopup.hide();
			autoCompletePopupShowing = false;
		}
	}

	private void setOnSuggestionListKeyPressed(ListView<String> suggestionsList, boolean insertMode,
			final String fQuery, final int fCaretPosition) {
		
		suggestionsList.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				if (suggestionsList.getSelectionModel().getSelectedItem() != null) {
					word.set(suggestionsList.getSelectionModel().getSelectedItem().toString());
				}
				else {
					word.set(suggestionsList.getItems().get(0).toString());
				}
	
				final String fWord = word.get();
				Platform.runLater(() -> {
					if (insertMode) {
						this.insertText(this.getCaretPosition(), fWord);
					} else {
						this.replaceText(fCaretPosition - fQuery.length(), fCaretPosition, fWord);
						this.moveTo(fCaretPosition + fWord.length() - fQuery.length());
					}
				});
				
				SqlCodeArea.this.hideAutocompletePopup();
				SqlCodeArea.this.autoCompleteAction(keyEvent);
			}
			if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.SPACE) {
				hideAutocompletePopup();
			}
		});
	}

	private void showAutoCompletePopup() {
		Bounds pointer = this.caretBoundsProperty().getValue().get();
		if (!autoCompletePopupShowing) {
			autoCompletePopup.show(this, pointer.getMaxX(), pointer.getMinY() + 20);
			autoCompletePopupShowing = true;
		}
	}

	private String calculateQuery(String ch, int caretPosition) {
		String query = "";
		ch = "";
		caretPosition--;
		do {
			ch = this.getText(caretPosition - 1, caretPosition--);
			query += ch;
		} while (!ch.equals(" ") && !(caretPosition == 0));

		query = StringUtils.reverse(query).trim();
		
		return query;
	}
	
	private Popup createPopup() {
		Popup popup = new Popup();
		popup.setAutoHide(true);
		popup.setOnAutoHide(event -> autoCompletePopupShowing = false);
		return popup;
	}

	@Override
	public StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = SqlCodeAreaSyntax.PATTERN.matcher(text);
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
													: matcher.group("STRING") != null ? "string"
															: matcher.group("COMMENT") != null ? "comment" : null;
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

	public boolean isAutoCompleteOnTypeEnabled() {
		return autoCompleteOnType;
	}

	public void setAutoCompleteOnType(boolean autoCompleteOnType) {
		this.autoCompleteOnType = autoCompleteOnType;
	}
	
	private static final int WORD_LENGTH_LIMIT = 45;

    public String getQuery(int position) {
        int limit = (position > WORD_LENGTH_LIMIT) ? WORD_LENGTH_LIMIT : position;
        String keywords = this.getText().substring(position - limit, position);
        //keywords = keywords.replaceAll("\\p{Punct}", " ").trim();
        keywords = keywords.replaceAll("\\n", " ").trim();
        int last = keywords.lastIndexOf(" ");
        return keywords.substring(last + 1);
    }

    public List<String> getQuerySuggestions(String query) {
        List<String> suggestions = SqlCodeAreaSyntax.KEYWORDS_lIST.parallelStream()
        							.filter(keyword -> keyword.startsWith(query)).collect(Collectors.toList());
//        suggestions.sort(Comparator.comparing(String::length).thenComparing(String::compareToIgnoreCase));
        return suggestions;
    }
    
    public List<String> getColumnsSuggestions(String table) {
    	for (String knownTable : tableAliases.keySet()) {
    		Collection<String> shortcuts = tableAliases.get(knownTable);
    		for (String s : shortcuts) {
    			if (s.trim().equals(table.trim()))
        	    	return SqlCodeAreaSyntax.COLUMNS_MAP.get(knownTable);
    		}
    	}
    	return SqlCodeAreaSyntax.COLUMNS_MAP.get(table);
    }

	@Override
	public void onChange(String newValue) {
		String[] split = newValue.split(">");
		String oldShortcut = split[0];
		String newShorcut = split[1];
		for (String table : tableAliases.keySet()) {
			if (tableAliases.get(table).remove(oldShortcut)) {
				tableAliases.get(table).add(newShorcut);
				return;
			}
		}
	}
	
	
	public void analyzeTextForTablesAliases(String text) {
		String[] words = text.split("\\W+");
		String saveTableShortcut = null;
		for (String word : words) {
			if (!word.isEmpty() && saveTableShortcut != null && !word.equals(saveTableShortcut.trim()) && !word.equals("as")) {
				System.out.println("Proccessing '" + word + "'");
				this.cacheTableAlias(word,saveTableShortcut);
				saveTableShortcut = null;
			}
			else if (SqlCodeAreaSyntax.COLUMNS_MAP.containsKey(word)) {
				System.out.println("Detected table '" + word + "'");
				if (!tableAliases.containsKey(word))
					tableAliases.put(word, new HashSet<>());
				saveTableShortcut = word;
			}
		}
	}

}