package com.planet_ink.coffee_mud.io;

import com.planet_ink.coffee_mud.Common.interfaces.Session;
import com.planet_ink.coffee_mud.MOBS.interfaces.MOB;
import com.planet_ink.coffee_mud.core.CMLib;
import com.planet_ink.coffee_mud.core.Log;
import com.planet_ink.coffee_mud.core.interfaces.Environmental;
import com.planet_ink.coffee_mud.core.interfaces.Physical;
import com.planet_ink.coffee_mud.io.interfaces.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultOutputFormatter implements OutputFormatter {

	private OutputHandler handler;
	private OutputFormattingContext formattingContext;
	private SnoopManager snoopManager;
	private IOExceptionHandler exceptionHandler;
	private OutputTranslator translator;
	private Session session;
	private MOB mob;
	private BlockingInputProvider inputProvider;

	private final AtomicBoolean lastWasPrompt = new AtomicBoolean(false);

	/*
	 * onlyPrint
	 */
	private final Object onlyPrint_spamLock = new Object();
	private final Object onlyPrint_cacheLock = new Object();

	private String lastStr = "";
	private int spamStack = 0;
	private StringBuffer curPrevMsg = null;
	private Vector<String> prevMsgs = new Vector<>();

	/*
	 * prompt state dependencies
	 */
	private boolean needPrompt;

	/*
	 * ad hoc filters
	 */
	protected List<Session.SessionFilter> textFilters = new Vector<Session.SessionFilter>(3);

	public DefaultOutputFormatter(OutputHandler handler, OutputFormattingContext formattingContext, SnoopManager snoopManager, IOExceptionHandler exceptionHandler) {
		this.handler = handler;
		this.formattingContext = formattingContext;
		this.snoopManager = snoopManager;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void setTranslator(OutputTranslator translator) {
		this.translator = translator;
	}

	@Override
	public void setBlockingInputProvider(BlockingInputProvider provider) {
		this.inputProvider = provider;
	}

	@Override
	public void setSession(Session session) {
		this.session = session;
	}

	@Override
	public void setMob(MOB mob) {
		this.mob = mob;
	}

	@Override
	public void rawCharsOut(char[] chars) {
		if ((handler == null) || (chars == null)) {
			return;
		}
		try {
			handler.rawCharsOut(chars);
		} catch (IOException e) {
			Log.errOut("OutputManager", "rawCharsOut: Error writing raw characters: " + e.getMessage());
			handleException(e);
		}
	}

	public void rawCharsOut(final String msg) {
		if (msg != null)
			rawCharsOut(msg.toCharArray());
	}

	public void rawCharsOut(final char c) {
		final char[] chars = {c};
		rawCharsOut(chars);
	}

	@Override
	public void rawOut(final String msg) {
		rawCharsOut(msg);
	}

	@Override
	public void onlyPrint(final String msg) {
		onlyPrint(msg, false);
	}

	@Override
	public void onlyPrint(String msg, boolean noCache) {
		if ((handler == null) || (msg == null)) {
			return;
		}
		try {
			snoopManager.snoopSupportPrint(msg, noCache);

			// Translation
			// TODO review hidden re-translate all messages
			if (translator != null) {
				String newMsg = translator.translate(msg);
				if (newMsg != null)
					msg = newMsg;
			}

			// Spam prevention
			synchronized (onlyPrint_spamLock) {
				if (msg.endsWith("\n\r") && msg.equals(lastStr) && msg.length() > 2 && msg.indexOf("\n") == (msg.length() - 2)) {
					spamStack++;
					return;
				}
				else if (spamStack > 0) {
					if (spamStack > 1)
						lastStr = lastStr.substring(0, lastStr.length() - 2) + "(" + spamStack + ")" + lastStr.substring(lastStr.length() - 2);
					rawCharsOut(lastStr.toCharArray());
				}
				spamStack = 0;

				if (msg.startsWith("\n\r") && msg.length() > 2)
					lastStr = msg.substring(2);
				else
					lastStr = msg;
			}

			// Paging logic
			final int pageBreak = formattingContext.getPageBreak();
			int lines = 0;
			int last = 0;
			if (pageBreak > 0)
				for (int i = 0; i < msg.length(); i++) {
					if (msg.charAt(i) == '\n') {
						lines++;
						if (lines >= pageBreak) {
							lines = 0;
							if ((i < (msg.length() - 1) && (msg.charAt(i + 1) == '\r')))
								i++;
							rawCharsOut(msg.substring(last, i + 1).toCharArray());
							last = i + 1;
							rawCharsOut("<pause - enter>".toCharArray());
							try {
								String s = inputProvider.blockingIn(-1, true);
								if (s != null) {
									s = s.toLowerCase();
									if (s.startsWith("qu") || s.startsWith("ex") || s.equals("x"))
										return;
								}
							} catch (final Exception e) {
								return;
							}
						}
					}
				}


			// Line caching
			if (!noCache) {
				synchronized (onlyPrint_cacheLock) {
					for (int i = 0; i < msg.length(); i++) {
						if (curPrevMsg == null)
							curPrevMsg = new StringBuffer("");
						if (msg.charAt(i) == '\r')
							continue;
						if (msg.charAt(i) == '\n') {
							if (curPrevMsg.toString().trim().length() > 0) {
								while (prevMsgs.size() >= OutputFormatter.MAX_PREVMSGS)
									prevMsgs.remove(0);
								formattingContext.addLastMsg(curPrevMsg.toString());
								curPrevMsg.setLength(0);
							}
							continue;
						}
						curPrevMsg.append(msg.charAt(i));
					}
				}
			}

			// Final output
			rawCharsOut(msg.toCharArray());
		} catch (Exception e) {
			Log.errOut("IOManager", "onlyPrint: Error: " + e.getMessage());
			handleException(e);
		}
	}

	@Override
	public void resetSpamStack() {
		synchronized (onlyPrint_spamLock) {
			this.lastStr = "";
			this.spamStack = 0;
		}
	}

	/*
	 * Raw Print Methods
	 */

	@Override
	public void rawPrint(final String msg) {
		if (msg == null)
			return;
		onlyPrint((needPrompt ? "" : (lastWasPrompt.get() ? "\n\r" : "")) + msg, false);
		flagTextDisplayed();
	}

	@Override
	public void rawPrintln(final String msg) {
		if (msg != null)
			rawPrint(msg + "\n\r");
	}

	@Override
	public void safeRawPrint(final String msg) {
		if (msg == null)
			return;
		onlyPrint((needPrompt ? "" : (lastWasPrompt.get() ? "\n\r" : "")) + CMLib.coffeeFilter().mxpSafetyFilter(msg, this.session), false);
		flagTextDisplayed();
	}

	@Override
	public void safeRawPrintln(final String msg) {
		if (msg != null)
			safeRawPrint(msg + "\n\r");
	}

	/*
	 * Standard Print Methods
	 */

	@Override
	public void print(final String msg) {
		onlyPrint(applyFilters(mob, mob, null, msg, false), false);
	}

	@Override
	public void println(final String msg) {
		if (msg != null)
			print(msg + "\n\r");
	}

	@Override
	public void stdPrint(final String msg) {
		rawPrint(applyFilters(mob, mob, null, msg, false));
	}

	@Override
	public void stdPrintln(final String msg) {
		if (msg != null)
			rawPrint(applyFilters(mob, mob, null, msg, false) + "\n\r");
	}

	/*
	 * Contextual Print Methods
	 */

	@Override
	public void print(final Physical src, final Environmental trg, final Environmental tol, final String msg) {
		onlyPrint(applyFilters(src, trg, tol, msg, false), false);
	}

	@Override
	public void println(final Physical src, final Environmental target, final Environmental tool, final String msg) {
		if (msg != null)
			onlyPrint(applyFilters(src, target, tool, msg, false) + "\n\r", false);
	}

	@Override
	public void stdPrint(final Physical src, final Environmental trg, final Environmental tol, final String msg) {
		rawPrint(applyFilters(src, trg, trg, msg, false));
	}

	@Override
	public void stdPrintln(final Physical src, final Environmental target, final Environmental tool, final String msg) {
		if (msg != null)
			rawPrint(applyFilters(src, target, tool, msg, false) + "\n\r");
	}

	/*
	 * Wrapless Print Methods
	 */

	@Override
	public void wraplessPrint(final String msg) {
		if (msg != null)
			onlyPrint(applyFilters(mob, mob, null, msg, true), false);
		flagTextDisplayed();
	}

	@Override
	public void wraplessPrintln(final String msg) {
		if (msg != null)
			onlyPrint(applyFilters(mob, mob, null, msg, true) + "\n\r", false);
		flagTextDisplayed();
	}

	/*
	 * Color-Only Print Methods
	 */

	@Override
	public void colorOnlyPrint(final String msg) {
		colorOnlyPrint(msg, false);
	}

	@Override
	public void colorOnlyPrintln(final String msg) {
		colorOnlyPrint(msg + "\n\r", false);
	}

	@Override
	public void colorOnlyPrint(final String msg, final boolean noCache) {
		if (msg != null)
			onlyPrint(CMLib.coffeeFilter().colorOnlyFilter(msg, this.session), noCache);
		flagTextDisplayed();
	}

	@Override
	public void colorOnlyPrintln(final String msg, final boolean noCache) {
		if (msg != null)
			onlyPrint(CMLib.coffeeFilter().colorOnlyFilter(msg, this.session) + "\n\r", noCache);
		flagTextDisplayed();
	}

	/*
	 * utility
	 */

	protected void flagTextDisplayed() {
		final MOB mob = this.mob;
		if ((mob != null) && (mob.isAttributeSet(MOB.Attrib.NOREPROMPT)) && (!mob.isInCombat()))
			return;
		needPrompt = true;
	}

	@Override
	public boolean addSessionFilter(final Session.SessionFilter filter) {
		if (filter == null)
			return false;
		if (!textFilters.contains(filter)) {
			this.textFilters.add(filter);
			return true;
		}
		return false;
	}

	protected String applyFilters(final Physical src, final Environmental trg, final Environmental tol, final String msg, final boolean noWrap) {
		if (msg != null) {
			if (!textFilters.isEmpty()) {
				String newMsg = msg;
				for (final Iterator<Session.SessionFilter> s = textFilters.iterator(); s.hasNext(); ) {
					final Session.SessionFilter filter = s.next();
					newMsg = filter.applyFilter(mob, src, trg, tol, newMsg);
					if (newMsg == null) {
						s.remove();
						return CMLib.coffeeFilter().fullOutFilter(this.session, mob, src, trg, tol, msg, noWrap);
					}
				}
				return CMLib.coffeeFilter().fullOutFilter(this.session, mob, src, trg, tol, newMsg, noWrap);
			}
			else
				return CMLib.coffeeFilter().fullOutFilter(this.session, mob, src, trg, tol, msg, noWrap);
		}
		return msg;
	}

	@Override
	public void setNeedPrompt(boolean value) {
		this.needPrompt = value;
	}

	@Override
	public boolean getNeedPrompt() {
		return this.needPrompt;
	}

	protected void handleException(Exception e) {
		if (exceptionHandler != null) {
			try {
				exceptionHandler.handleException(e);
			} catch (Exception callbackException) {
				Log.errOut("IOManager", "handleException: Error in IOExceptionHandler: " + callbackException.getMessage());
			}
		}
		else {
			Log.errOut("IOManager", "handleException: Unhandled exception: " + e.getMessage());
		}
	}

	@Override
	public void shutdown() throws IOException {
		try {
			needPrompt = false;

			textFilters.clear();

			synchronized (onlyPrint_cacheLock) {
				prevMsgs.clear();
				if (curPrevMsg != null) {
					curPrevMsg.setLength(0);
				}
			}

			resetSpamStack();

			inputProvider = null;
			mob = null;
			session = null;
			exceptionHandler = null;
			translator = null;
			snoopManager = null;
			formattingContext = null;
			handler = null;

			Log.sysOut("DefaultOutputFormatter", "shutdown: Output formatter shut down successfully.");
		} catch (Exception e) {
			Log.errOut("DefaultOutputFormatter", "shutdown: Error during shutdown: " + e.getMessage());
			throw new IOException("Error during OutputFormatter shutdown", e);
		}
	}
}
