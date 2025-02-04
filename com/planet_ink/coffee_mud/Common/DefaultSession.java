package com.planet_ink.coffee_mud.Common;

import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.threads.CMRunnable;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.CMSecurity.DbgFlag;
import com.planet_ink.coffee_mud.core.CMSecurity.DisFlag;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.CharCreationLibrary.LoginResult;
import com.planet_ink.coffee_mud.Libraries.interfaces.CharCreationLibrary.LoginSession;
import com.planet_ink.coffee_mud.Libraries.interfaces.ColorLibrary.ColorState;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.MOB.Attrib;
import com.jcraft.jzlib.*;

import com.planet_ink.coffee_mud.core.Log;

import com.planet_ink.coffee_mud.io.interfaces.*;
import com.planet_ink.coffee_mud.io.*;
import com.planet_ink.coffee_mud.session.DefaultCommandManager;
import com.planet_ink.coffee_mud.session.interfaces.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.net.*;
import java.nio.charset.Charset;

/*
   Copyright 2025 github.com/toasted323
   Copyright 2005-2024 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   CHANGES:
   2025-02 toasted323: Implement shutdown process for OutputFormatter
   2025-02 toasted323: Remove output methods from Session interface
   2025-02 toasted323: Extract OutputFormatter from DefaultSession
   2025-02 toasted323: Move I/O handlers and interfaces to dedicated package
   2025-02 toasted323: Remove low-level stream handling leftovers from DefaultSession
   2025-02 toasted323: Implement shutdown methods in IO handlers
   2025-02 toasted323: Extract write timing logic into OutputHandler
   2025-02 toasted323: Extract char-level I/O methods into Input- and OutputHandler interfaces
   2025-02 toasted323: Remove unused OutputStream parameter from rawBytesOut
   2025-02 toasted323: Introduce lightweight termination mechanism for Input- and OutputHandler
   2025-02 toasted323: Introduced Input- and OutputHandler interfaces for standardized byte I/O operations
   2024-12 toasted323: Ensure any exit changes observed by the player are sent via gmcp too
   2024-12 toasted323: Mapping from ships
*/

public class DefaultSession implements Session, CommandManagerContext, OutputFormattingContext, SnoopManager, BlockingInputProvider
{
	protected static final int		SOTIMEOUT		= 300;
	protected static final int		PINGTIMEOUT  	= 30000;
	protected static final int		MSDPPINGINTERVAL= 1000;
	protected static final char[]	PINGCHARS		= {0};
	protected static final String	TIMEOUT_MSG		= "Timed Out.";

	protected final Set<Integer>		telnetSupportSet= new HashSet<Integer>();
	protected final Set<String>			mxpSupportSet	= new HashSet<String>();
	protected final Map<String,String>	mxpVersionInfo  = new Hashtable<String,String>();
	protected final Map<Object, Object> msdpReportables = new TreeMap<Object,Object>();
	protected final Map<String, Double> gmcpSupports	= new TreeMap<String,Double>();
	protected final Map<String, Long> 	gmcpPings		= new TreeMap<String,Long>();

	protected final String[]			mcpKey			= new String[1];
	protected final Map<String,String>	mcpKeyPairs		= new TreeMap<String,String>();
	protected final boolean		 		mcpDisabled		= CMSecurity.isDisabled(DisFlag.MCP);
	protected final Map<String,float[]>	mcpSupported	= new TreeMap<String,float[]>();
	protected final AtomicBoolean socketShutdownLock = new AtomicBoolean(false);
	protected final LinkedList<List<String>>history		= new LinkedList<List<String>>();

	private volatile Thread  runThread 			 = null;
	private volatile Thread	 writeThread 		 = null;
	protected String		 groupName			 = null;
	protected SessionStatus  status 			 = SessionStatus.HANDSHAKE_OPEN;
	protected volatile long  lastStateChangeMs	 = System.currentTimeMillis();
	protected int   		 snoopSuspensionStack= 0;
	protected Socket 	 	 sock				 = null;
	protected InputStream    rawin;
	protected OutputStream   rawout;
	protected MOB   		 mob;
	protected PlayerAccount  acct				 = null;
	protected boolean   	 killFlag			 = false;
	protected boolean   	 needPrompt			 = false;
	protected long		 	 afkTime			 = 0;
	protected String		 afkMessage			 = null;
	protected StringBuffer   input				 = new StringBuffer("");
	protected StringBuffer   fakeInput			 = null;
	protected boolean   	 waiting			 = false;
	protected String[]  	 clookup			 = null;
	protected String		 lastColorStr		 = "";
	protected List<Session>	 snoops				 = new Vector<Session>();
	protected List<String>	 prevMsgs			 = new Vector<String>();
	protected boolean		 lastWasCR			 = false;
	protected boolean		 lastWasLF			 = false;
	protected boolean		 suspendCommandLine	 = false;
	protected boolean[] 	 serverTelnetCodes	 = new boolean[256];
	protected boolean[]		 clientTelnetCodes	 = new boolean[256];
	protected String		 terminalType		 = "UNKNOWN";
	protected int			 terminalWidth		 = -1;
	protected int			 terminalHeight		 = -1;
	protected boolean		 bNextByteIs255		 = false;
	protected boolean		 connectionComplete	 = false;
	protected ReentrantLock  writeLock 			 = new ReentrantLock(true);
	protected LoginSession	 loginSession 		 = null;
	protected boolean[]		 loggingOutObj		 = new boolean[]{false};

	protected byte[]		 promptSuffix		 = new byte[0];
	protected ColorState	 currentColor		 = null;
	protected ColorState	 lastColor			 = null;
	protected long			 lastLoopTop		 = System.currentTimeMillis();
	protected long			 nextMsdpPing		 = System.currentTimeMillis();
	protected long			 userLoginTime		 = System.currentTimeMillis();
	protected long			 onlineTime			 = System.currentTimeMillis();
	protected long			 activeMillis		 = 0;
	protected long			 lastPKFight		 = 0;
	protected long			 lastNPCFight		 = 0;
	protected long			 lastBlahCheck		 = 0;
	protected long			 tickTotal			 = 0;
	protected long			 lastKeystroke		 = 0;
	protected volatile long	 lastIACIn		 	 = System.currentTimeMillis();
	protected long			 promptLastShown	 = 0;
	protected char 			 threadGroupChar	 = '\0';
	protected boolean		 debugStrInput		 = false;
	protected boolean		 debugStrOutput		 = false;
	protected boolean		 debugBinOutput		 = false;
	protected boolean		 debugBinInput		 = false;
	protected StringBuffer   debugBinInputBuf	 = new StringBuffer("");

	protected final Stack<ColorState>	markedColors	= new Stack<ColorState>();
	protected AtomicBoolean				lastWasPrompt	= new AtomicBoolean(false);
	protected volatile InputCallback	inputCallback	= null;
	protected String 					lastSeenRoomID 	= null;
	protected Integer 					lastRoomHash 	= null;

	private InputHandler inputHandler = null;
	private OutputHandler outputHandler = null;
	private DefaultOutputFormatter outputFormatter;
	private IOExceptionHandler exceptionHandler;
	private CommandManager commandManager;


	@Override
	public String ID()
	{
		return "DefaultSession";
	}

	@Override
	public String name()
	{
		return ID();
	}

	@Override
	public CMObject newInstance()
	{
		try
		{
			return getClass().getDeclaredConstructor().newInstance();
		}
		catch (final Exception e)
		{
			return new DefaultSession();
		}
	}

	@Override
	public void initializeClass()
	{
	}

	@Override
	public boolean isFake()
	{
		return false;
	}

	@Override
	public CMObject copyOf()
	{
		try
		{
			final Object O = this.clone();
			return (CMObject) O;
		}
		catch (final Exception e)
		{
			return newInstance();
		}
	}

	@Override
	public int compareTo(final CMObject o)
	{
		return CMClass.classID(this).compareToIgnoreCase(CMClass.classID(o));
	}

	public DefaultSession()
	{
		threadGroupChar=Thread.currentThread().getThreadGroup().getName().charAt(0);
	}

	@Override
	public void setStatus(final SessionStatus newStatus)
	{
		synchronized(status)
		{
			if(status!=newStatus)
			{
				status=newStatus;
				lastStateChangeMs=System.currentTimeMillis();
			}
		}
	}

	@Override
	public long getStartTime()
	{
		return commandManager.getProcessingStartTimeMillis();
	}

	@Override
	public int getGroupID()
	{
		return this.threadGroupChar;
	}

	@Override
	public boolean isAllowedMcp(final String packageName, final float version)
	{
		final float[] chk = mcpSupported.get(packageName);
		if((chk == null)||(version < chk[0])||(version>chk[1])||(mcpKey[0] == null))
		{
			return false;
		}
		return true;
	}

	@Override
	public boolean sendMcpCommand(final String packageCommand, final String parms)
	{
		if(mcpKey[0] != null)
		{
			outputFormatter.rawPrintln("#$#"+packageCommand+" "+mcpKey[0]+" "+parms);
			return true;
		}
		return false;
	}

	private CommandManager createCommandManager() {
		InputParser inputParser = input -> CMParms.parse(input);

		PlayerAliasExpander aliasExpander = (rawAliasDefinition, parsedInput, executableCommands, doEcho) ->
				CMLib.utensils().deAlias(rawAliasDefinition, parsedInput, executableCommands, doEcho);

		CommandTranslator commandTranslator = CMLib.lang()::preCommandParser;

		PlayerCommandMetaFlags metaFlagsProvider = this::metaFlags;

		return new DefaultCommandManager(
				this,
				inputParser,
				outputFormatter,
				aliasExpander,
				commandTranslator,
				metaFlagsProvider
		);
	}

	@Override
	public void initializeSession(final Socket s, final String groupName, final String introTextStr)
	{
		this.groupName=groupName;
		sock=s;
		promptSuffix = CMProps.getPromptSuffix();
		currentColor = CMLib.color().getNormalColor();
		lastColor = CMLib.color().getNormalColor();
		markedColors.clear();
		try
		{
			setStatus(SessionStatus.HANDSHAKE_OPEN);
			debugStrInput = CMSecurity.isDebugging(CMSecurity.DbgFlag.INPUT);
			debugBinOutput = CMSecurity.isDebugging(CMSecurity.DbgFlag.BINOUT);
			debugStrOutput = CMSecurity.isDebugging(CMSecurity.DbgFlag.STROUT);
			debugBinInput = CMSecurity.isDebugging(CMSecurity.DbgFlag.BININ);
			if(debugBinInput)
			{
				CMLib.threads().startTickDown(new Tickable()
				{
					@Override
					public String ID()
					{
						return "SessionTicker";
					}

					@Override
					public CMObject newInstance()
					{
						return null;
					}

					@Override
					public CMObject copyOf()
					{
						return null;
					}

					@Override
					public void initializeClass()
					{
					}

					@Override
					public int compareTo(final CMObject o)
					{
						return 0;
					}

					@Override
					public String name()
					{
						return ID();
					}

					@Override
					public int getTickStatus()
					{
						return 0;
					}

					@Override
					public boolean tick(final Tickable ticking, final int tickID)
					{
						if(debugBinInputBuf.length()>0)
						{
							Log.sysOut("BINPUT: "+(mob==null?"":mob.Name())+": '"+debugBinInputBuf.toString()+"'");
							debugBinInputBuf.setLength(0);
						}
						return !killFlag;
					}
				}, 0, 1, 1);
			}

			final Charset inputCharSet=Charset.forName(CMProps.getVar(CMProps.Str.CHARSETINPUT));
			final Charset outputCharSet=Charset.forName(CMProps.getVar(CMProps.Str.CHARSETOUTPUT));

			OutputTranslator cmLibTranslator = message -> CMLib.lang().finalTranslation(message);

			exceptionHandler = e -> {
				Log.errOut("DefaultSession", "handleException: I/O Exception: " + e.getMessage());
				setKillFlag(true);
			};

			if((sock == null)||(!sock.isConnected()))
			{
				rawin=new BufferedInputStream(new ByteArrayInputStream(new byte[0]));
				inputHandler = new DefaultInputHandler(rawin, inputCharSet, debugStrInput, debugBinInput);
				rawout=new BufferedOutputStream(new ByteArrayOutputStream());
				outputHandler = new DefaultOutputHandler(rawout, outputCharSet, writeLock, debugStrOutput, debugStrOutput);
				outputFormatter = new DefaultOutputFormatter(outputHandler, this,this, exceptionHandler);
				commandManager = createCommandManager();
				return;
			}

			sock.setSoTimeout(SOTIMEOUT);
			rawin=new BufferedInputStream(sock.getInputStream());
			inputHandler = new DefaultInputHandler(rawin, inputCharSet, debugStrInput, debugBinInput);
			rawout=new BufferedOutputStream(sock.getOutputStream());
			outputHandler = new DefaultOutputHandler(rawout, outputCharSet, writeLock, debugStrOutput, debugStrOutput);
			outputFormatter = new DefaultOutputFormatter(outputHandler, this,this, exceptionHandler);
			outputFormatter.setTranslator(cmLibTranslator);
			outputFormatter.setBlockingInputProvider(this);
			outputFormatter.setSession(this);
			outputFormatter.setMob(this.mob);
			commandManager = createCommandManager();

			if(!mcpDisabled)
			{
				rawBytesOut(("\n\r#$#mcp version: 2.1 to: 2.1\n\r").getBytes(CMProps.getVar(CMProps.Str.CHARSETOUTPUT)));
			}
			rawBytesOut(("\n\rConnecting to "+CMProps.getVar(CMProps.Str.MUDNAME)+"...\n\r").getBytes("US-ASCII"));

			setServerTelnetMode(TELNET_ANSI,true);
			setClientTelnetMode(TELNET_ANSI,true);
			setServerTelnetMode(TELNET_ANSI16,false);
			setClientTelnetMode(TELNET_ANSI16,false);
			setClientTelnetMode(TELNET_TERMTYPE,true);
			changeTelnetMode(rawout,TELNET_TERMTYPE,true);
			negotiateTelnetMode(rawout,TELNET_TERMTYPE);
			if(!CMSecurity.isDisabled(CMSecurity.DisFlag.MCCP))
				changeTelnetMode(rawout,TELNET_COMPRESS2,true);
			if(!CMSecurity.isDisabled(CMSecurity.DisFlag.MXP))
				changeTelnetMode(rawout,TELNET_MXP,true);
			if(!CMSecurity.isDisabled(CMSecurity.DisFlag.GMCP))
				changeTelnetMode(rawout,TELNET_GMCP,true);
			if(!CMSecurity.isDisabled(CMSecurity.DisFlag.MSP))
				changeTelnetMode(rawout,TELNET_MSP,true);
			if(!CMSecurity.isDisabled(CMSecurity.DisFlag.MSDP))
				changeTelnetMode(rawout,TELNET_MSDP,true);
			//changeTelnetMode(rawout,TELNET_SUPRESS_GO_AHEAD,true);
			changeTelnetMode(rawout,TELNET_NAWS,true);
			//changeTelnetMode(rawout,TELNET_BINARY,true);
			if(mightSupportTelnetMode(TELNET_GA))
				rawBytesOut(TELNETGABYTES);
			if((!CMSecurity.isDisabled(CMSecurity.DisFlag.MSSP))
			&&(mightSupportTelnetMode(TELNET_MSSP)))
				changeTelnetMode(rawout,TELNET_MSSP,true);


			//final DefaultSession me = this;
			prompt(new TickingCallback(250)
			{
				private final long firstIACIn=lastIACIn;
				//final DefaultSession sess = me;

				@Override
				public boolean tick(final int counter)
				{
					try
					{
						if(outputHandler==null) {
							setKillFlag(true);
							return false;
						}
						switch(status)
						{
						case HANDSHAKE_OPEN:
						{
							if((!terminalType.equals("ANSI"))&&(getClientTelnetMode(TELNET_ECHO)))
								changeTelnetModeBackwards(rawout,TELNET_ECHO,false);
							setStatus(SessionStatus.HANDSHAKE_MCCP);
							break;
						}
						case HANDSHAKE_MCCP:
						{
							if(((lastIACIn>firstIACIn)&&((System.currentTimeMillis()-lastIACIn)>500))
							||((System.currentTimeMillis()-lastIACIn)>5000)
							||((System.currentTimeMillis()-lastStateChangeMs)>10000))
							{
								if(getClientTelnetMode(TELNET_COMPRESS2))
								{
									negotiateTelnetMode(rawout,TELNET_COMPRESS2);
									if(getClientTelnetMode(TELNET_COMPRESS2))
									{
										final ZOutputStream zOut=new ZOutputStream(rawout, JZlib.Z_DEFAULT_COMPRESSION);
										rawout=zOut;
										outputHandler.resetByteStream(rawout);
										zOut.setFlushMode(JZlib.Z_SYNC_FLUSH);
										if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
											Log.debugOut("MCCP compression started");
									}
								}
								setStatus(SessionStatus.HANDSHAKE_MXP);
							}
							break;
						}
						case HANDSHAKE_MXP:
						{
							if(!getClientTelnetMode(Session.TELNET_MXP))
								setStatus(SessionStatus.HANDSHAKE_DONE);
							else
							{
								outputFormatter.rawOut("\n\033[6z\n\033[6z<SUPPORT IMAGE IMAGE.URL>\n");
								rawout.flush();
								outputFormatter.rawOut("\n\033[6z\n\033[6z<SUPPORT>\n");
								rawout.flush();
								setStatus(SessionStatus.HANDSHAKE_MXPPAUSE);
							}
							break;
						}
						case HANDSHAKE_MXPPAUSE:
						{
							if(((System.currentTimeMillis()-lastStateChangeMs)>3000)
							||(mxpSupportSet.contains("+IMAGE.URL")||mxpSupportSet.contains("+IMAGE")||mxpSupportSet.contains("-IMAGE.URL")))
								setStatus(SessionStatus.HANDSHAKE_DONE);
							break;
						}
						case HANDSHAKE_DONE:
						{
							if(introTextStr!=null)
								outputFormatter.print(introTextStr);
							if(outputHandler!=null)
							{
								if((getClientTelnetMode(Session.TELNET_MXP))
								&&((mxpSupportSet.contains("+IMAGE.URL"))
									||((mxpSupportSet.contains("+IMAGE"))&&(!mxpSupportSet.contains("-IMAGE.URL")))))
								{
									// also the intro page
									final String[] paths=CMLib.protocol().mxpImagePath("intro.jpg");
									if(paths[0].length()>0)
									{
										final CMFile introDir=new CMFile("/web/pub/images/mxp",null,CMFile.FLAG_FORCEALLOW);
										String introFilename=paths[1];
										if(introDir.isDirectory())
										{
											final CMFile[] files=introDir.listFiles();
											final List<String> choices=new ArrayList<String>();
											for (final CMFile file : files)
											{
												if(file.getName().toLowerCase().startsWith("intro")
												&&file.getName().toLowerCase().endsWith(".jpg"))
													choices.add(file.getName());
											}
											if(choices.size()>0)
												introFilename=choices.get(CMLib.dice().roll(1,choices.size(),-1));
										}
										outputFormatter.println("\n\r\n\r\n\r^<IMAGE '"+introFilename+"' URL='"+paths[0]+"' H=400 W=400^>\n\r\n\r");
									}
								}
							}
						}
							//$FALL-THROUGH$
						default:
							connectionComplete=true;
							status=SessionStatus.LOGIN;
							if(collectedInput.length()>0)
								fakeInput=new StringBuffer(collectedInput.toString());
							return false;
						}
						return (outputHandler!=null);
					}
					catch(final Exception e)
					{
						if(e.getMessage()==null)
							Log.errOut(e);
						else
							Log.errOut(e.getMessage());
					}
					connectionComplete=true;
					status=SessionStatus.LOGIN;
					return false;
				}
			});
		}
		catch(final Exception e)
		{
			if(e.getMessage()==null)
				Log.errOut(e);
			else
				Log.errOut(e.getMessage());
		}
	}

	protected void compress2Off() throws IOException
	{
		changeTelnetMode(rawout,TELNET_COMPRESS2,false);
		rawout=new BufferedOutputStream(sock.getOutputStream());
		outputHandler.resetByteStream(rawout);
		CMLib.s_sleep(50);
		changeTelnetMode(rawout,TELNET_COMPRESS2,false);
		if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
			Log.debugOut("MCCP compression stopped");
	}

	@Override
	public String getGroupName()
	{
		return groupName;
	}

	@Override
	public void setGroupName(final String group)
	{
		groupName=group;
		threadGroupChar=groupName.charAt(0);
	}

	@Override
	public void setFakeInput(final String input)
	{
		if(fakeInput!=null)
			fakeInput.append(input);
		else
			fakeInput=new StringBuffer(input);
	}

	private void negotiateTelnetMode(final OutputStream out, final int optionCode)
	throws IOException
	{
		if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
			Log.debugOut("Sent sub-option: "+Session.TELNET_DESCS[optionCode]);
		if(optionCode==TELNET_TERMTYPE)
		{
			final byte[] stream={(byte)TELNET_IAC,(byte)TELNET_SB,(byte)optionCode,(byte)1,(byte)TELNET_IAC,(byte)TELNET_SE};
			rawBytesOut(stream);
		}
		else
		{
			final byte[] stream={(byte)TELNET_IAC,(byte)TELNET_SB,(byte)optionCode,(byte)TELNET_IAC,(byte)TELNET_SE};
			rawBytesOut(stream);
		}
		//out.flush(); rawBytesOut already flushes
	}

	private boolean mightSupportTelnetMode(final int telnetCode)
	{
		if(telnetSupportSet.size()==0)
		{
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_MXP));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_MSP));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_MSDP));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_MSSP));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_GMCP));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_TERMTYPE));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_BINARY));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_ECHO));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_LOGOUT));
			telnetSupportSet.add(Integer.valueOf(Session.TELNET_NAWS));
			//telnetSupportSet.add(Integer.valueOf(Session.TELNET_GA));
			//telnetSupportSet.add(Integer.valueOf(Session.TELNET_SUPRESS_GO_AHEAD));
			//telnetSupportSet.add(Integer.valueOf(Session.TELNET_COMPRESS2));
			//telnetSupportSet.add(Integer.valueOf(Session.TELNET_LINEMODE));
		}
		return telnetSupportSet.contains(Integer.valueOf(telnetCode));
	}

	@Override
	public void setServerTelnetMode(final int telnetCode, final boolean onOff)
	{
		serverTelnetCodes[telnetCode]=onOff;
	}

	@Override
	public boolean getServerTelnetMode(final int telnetCode)
	{
		return serverTelnetCodes[telnetCode];
	}

	@Override
	public void setClientTelnetMode(final int telnetCode, final boolean onOff)
	{
		clientTelnetCodes[telnetCode]=onOff;
	}

	@Override
	public boolean getClientTelnetMode(final int telnetCode)
	{
		return clientTelnetCodes[telnetCode];
	}

	private void changeTelnetMode(final OutputStream out, final int telnetCode, final boolean onOff) throws IOException
	{
		final byte[] command;
		if(telnetCode == TELNET_TERMTYPE)
			command=new byte[]{(byte)TELNET_IAC,onOff?(byte)TELNET_DO:(byte)TELNET_DONT,(byte)telnetCode};
		else
			command=new byte[]{(byte)TELNET_IAC,onOff?(byte)TELNET_WILL:(byte)TELNET_WONT,(byte)telnetCode};
		rawBytesOut(command);
		if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
			Log.debugOut("Sent: "+(onOff?"Will":"Won't")+" "+Session.TELNET_DESCS[telnetCode]);
		setServerTelnetMode(telnetCode,onOff);
	}

	@Override
	public boolean isAllowedMxp(final String tagString)
	{
		if(tagString.startsWith("^<"))
		{
			if((!getClientTelnetMode(TELNET_MXP))||(mxpSupportSet.size()==0))
				return false;
			int x=tagString.indexOf(' ');
			if(x<0)
				x=tagString.indexOf('>');
			if((x>0)&&(this.mxpSupportSet.contains("-"+tagString.substring(2,x))))
				return false;
		}
		else
		if(tagString.startsWith("&"))
		{
			if(terminalType.equalsIgnoreCase("mushclient"))
				return true;
		}
		return true;
	}

	@Override
	public boolean sendGMCPEvent(final String eventName, final String json)
	{
		if((!getClientTelnetMode(TELNET_GMCP))||(gmcpSupports.size()==0))
			return false;
		try
		{
			final String lowerEventName=eventName.toLowerCase().trim();
			final int x=lowerEventName.lastIndexOf('.');
			if((x<0)&&(!gmcpSupports.containsKey(lowerEventName)))
				return false;
			if((!gmcpSupports.containsKey(lowerEventName))
			&& (!gmcpSupports.containsKey(lowerEventName.substring(0, x))))
				return false;
			if(CMSecurity.isDebugging(DbgFlag.TELNET))
				Log.debugOut("GMCP Sent: "+(lowerEventName+" "+json));
			final ByteArrayOutputStream bout=new ByteArrayOutputStream();
			bout.write(TELNETBYTES_GMCP_HEAD);
			bout.write((lowerEventName+" "+json).getBytes());
			bout.write(TELNETBYTES_END_SB);
			synchronized(gmcpSupports)
			{
				rawBytesOut(bout.toByteArray());
			}
			return true;
		}
		catch(final IOException e)
		{
			setKillFlag(true);
		}
		catch(final Exception e)
		{
			Log.errOut(e);
		}
		return false;
	}

	// this is stupid, but a printwriter can not be cast as an outputstream, so this dup was necessary
	@Override
	public void changeTelnetMode(final int telnetCode, final boolean onOff)
	{
		try
		{
			final byte[] command={(byte)TELNET_IAC,onOff?(byte)TELNET_WILL:(byte)TELNET_WONT,(byte)telnetCode};
			rawBytesOut(command);
		}
		catch (final Exception e)
		{
		}
		if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
			Log.debugOut("Sent: "+(onOff?"Will":"Won't")+" "+Session.TELNET_DESCS[telnetCode]);
		setServerTelnetMode(telnetCode,onOff);
	}

	public void changeTelnetModeBackwards(final int telnetCode, final boolean onOff) throws IOException
	{
		final byte[] command={(byte)TELNET_IAC,onOff?(byte)TELNET_DO:(byte)TELNET_DONT,(byte)telnetCode};
		rawBytesOut(command);
		if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
			Log.debugOut("Back-Sent: "+(onOff?"Do":"Don't")+" "+Session.TELNET_DESCS[telnetCode]);
		setServerTelnetMode(telnetCode,onOff);
	}

	public void changeTelnetModeBackwards(final OutputStream out, final int telnetCode, final boolean onOff) throws IOException
	{
		final byte[] command={(byte)TELNET_IAC,onOff?(byte)TELNET_DO:(byte)TELNET_DONT,(byte)telnetCode};
		rawBytesOut(command);
		if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
			Log.debugOut("Back-Sent: "+(onOff?"Do":"Don't")+" "+Session.TELNET_DESCS[telnetCode]);
		setServerTelnetMode(telnetCode,onOff);
	}

	@Override
	public void negotiateTelnetMode(final int telnetCode)
	{
		try
		{
			if(telnetCode==TELNET_TERMTYPE)
			{
				final byte[] command={(byte)TELNET_IAC,(byte)TELNET_SB,(byte)telnetCode,(byte)1,(byte)TELNET_IAC,(byte)TELNET_SE};
				rawBytesOut(command);
			}
			else
			{
				final byte[] command={(byte)TELNET_IAC,(byte)TELNET_SB,(byte)telnetCode,(byte)TELNET_IAC,(byte)TELNET_SE};
				rawBytesOut(command);
			}
		}
		catch (final Exception e)
		{
		}
		if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
			Log.debugOut("Negotiate-Sent: "+Session.TELNET_DESCS[telnetCode]);
	}

	private void mushClientTurnOffMXPFix()
	{
		try
		{
			// i don't know why this works, but it does
			clearInputBuffer(500);
			Command C=CMClass.getCommand("MXP");
			C.execute(mob, new XVector<String>("MXP","QUIET"), 0);
			clearInputBuffer(500);
			C=CMClass.getCommand("NOMXP");
			C.execute(mob, new XVector<String>("NOMXP","QUIET"), 0);
		}
		catch(final Exception e)
		{
		}
	}

	@Override
	public void initTelnetMode(final long attributesBitmap)
	{
		setServerTelnetMode(TELNET_ANSI,CMath.bset(attributesBitmap,MOB.Attrib.ANSI.getBitCode()));
		setClientTelnetMode(TELNET_ANSI,CMath.bset(attributesBitmap,MOB.Attrib.ANSI.getBitCode()));
		setServerTelnetMode(TELNET_ANSI16,CMath.bset(attributesBitmap,MOB.Attrib.ANSI16.getBitCode()));
		setClientTelnetMode(TELNET_ANSI16,CMath.bset(attributesBitmap,MOB.Attrib.ANSI16.getBitCode()));
		boolean changedSomething=false;
		final boolean mxpSet=(!CMSecurity.isDisabled(CMSecurity.DisFlag.MXP))&&CMath.bset(attributesBitmap,MOB.Attrib.MXP.getBitCode());
		if(mxpSet!=getClientTelnetMode(TELNET_MXP))
		{
			changeTelnetMode(TELNET_MXP,!getClientTelnetMode(TELNET_MXP));
			changedSomething=true;
			if((!mxpSet)&&(getTerminalType()!=null)&&(getTerminalType().equals("mushclient")))
				mushClientTurnOffMXPFix();
		}
		final boolean mspSet=(!CMSecurity.isDisabled(CMSecurity.DisFlag.MSP))&&CMath.bset(attributesBitmap,MOB.Attrib.SOUND.getBitCode());
		if(mspSet!=getClientTelnetMode(TELNET_MSP))
		{
			changeTelnetMode(TELNET_MSP,!getClientTelnetMode(TELNET_MSP));
			changedSomething=true;
		}
		if(changedSomething)
			clearInputBuffer(500);
	}

	@Override
	public ColorState getCurrentColor()
	{
		return currentColor;
	}

	@Override
	public void setCurrentColor(final ColorState newColor)
	{
		if(newColor!=null)
			currentColor=newColor;
	}

	@Override
	public ColorState getLastColor()
	{
		return lastColor;
	}

	@Override
	public void setLastColor(final ColorState newColor)
	{
		if(newColor!=null)
			lastColor=newColor;
	}

	@Override
	public ColorState popMarkedColor()
	{
		if(markedColors.size()==0)
			return CMLib.color().getNormalColor();
		return markedColors.pop();
	}

	@Override
	public void pushMarkedColor(final ColorState newColor)
	{
		if(newColor!=null)
			markedColors.push(newColor);
	}

	@Override
	public long getTotalMillis()
	{
		return commandManager.getTotalProcessingTimeMillis();
	}

	@Override
	public long getIdleMillis()
	{
		return System.currentTimeMillis() - lastKeystroke;
	}

	@Override
	public long getTotalTicks()
	{
		return tickTotal;
	}

	@Override
	public long getMillisOnline()
	{
		return System.currentTimeMillis() - onlineTime;
	}

	@Override
	public long getInputLoopTime()
	{
		return lastLoopTop;
	}

	@Override
	public void setInputLoopTime()
	{
		lastLoopTop = System.currentTimeMillis();
	}

	@Override
	public long getLastPKFight()
	{
		return lastPKFight;
	}

	@Override
	public void setLastPKFight()
	{
		lastPKFight = System.currentTimeMillis();
	}

	@Override
	public long getLastNPCFight()
	{
		return lastNPCFight;
	}

	@Override
	public void setLastNPCFight()
	{
		lastNPCFight = System.currentTimeMillis();
	}

	@Override
	public List<String> getLastMsgs()
	{
		return new XVector<String>(prevMsgs);
	}

	@Override
	public void addLastMsg(String msg)
	{
		prevMsgs.add(msg);
	}

	@Override
	public String getTerminalType()
	{
		return terminalType;
	}

	@Override
	public MOB mob()
	{
		return mob;
	}

	@Override
	public void setMob(final MOB newmob)
	{
		mob=newmob;
	}

	@Override
	public void setAccount(final PlayerAccount account)
	{
		acct=account;
	}

	@Override
	public int getWrap()
	{
		if(terminalWidth>5)
			return terminalWidth;
		return ((mob!=null)&&(mob.playerStats()!=null))?mob.playerStats().getWrap():78;
	}

	@Override
	public int getPageBreak()
	{
		if(((mob!=null)&&(mob.playerStats()!=null)))
		{
			final int pageBreak=mob.playerStats().getPageBreak();
			if(pageBreak <= 0)
				return pageBreak;
			if(terminalHeight>3)
				return terminalHeight;
			return pageBreak;
		}
		return -1;
	}

	@Override
	public int getTerminalHeight() {
		return terminalHeight;
	}

	@Override
	public int getTerminalWidth() {
		return terminalWidth;
	}

	@Override
	public boolean isStopped()
	{
		return killFlag;
	}

	public void setKillFlag(final boolean truefalse)
	{
		if(killFlag != truefalse) {
			killFlag = truefalse;
			if (killFlag) {
				inputHandler.requestTermination();
				outputHandler.requestTermination();
			}
		}
	}

	private static final List<String> empty = new ReadOnlyList<String>(new ArrayList<String>(0));

	@Override
	public List<String> getPreviousCMD()
	{
		synchronized(history)
		{
			if(history.size()==0)
				return empty;
			return history.getLast();
		}
	}

	@Override
	public void setBeingSnoopedBy(final Session session, final boolean onOff)
	{
		if(onOff)
		{
			if(!snoops.contains(session))
				snoops.add(session);
		}
		else
		{
			while(snoops.contains(session))
				snoops.remove(session);
		}
	}

	@Override
	public boolean isBeingSnoopedBy(final Session S)
	{
		if(S==null)
			return snoops.size()==0;
		return(snoops.contains(S));
	}

	@Override
	public synchronized int snoopSuspension(final int change)
	{
		snoopSuspensionStack+=change;
		return snoopSuspensionStack;
	}

	@Override
	public Enumeration<List<String>> getHistory()
	{
		final Vector<List<String>> V;
		synchronized(history)
		{
			V = new XVector<List<String>>(history.descendingIterator());
		}
		return V.elements();
	}

	private int metaFlags()
	{
		return ((snoops.size()>0)?MUDCmdProcessor.METAFLAG_SNOOPED:0)
			   |(((mob!=null)&&(mob.soulMate()!=null))?MUDCmdProcessor.METAFLAG_POSSESSED:0);
	}

	@Override
	public void setPreviousCmd(final List<String> cmds)
	{
		if(cmds==null)
			return;
		if(cmds.size()==0)
			return;
		if((cmds.size()>0)&&(cmds.get(0).trim().startsWith("!")))
			return;

		synchronized(history)
		{
			history.addLast(new XVector<String>(cmds));
			if(history.size()>100)
				history.removeFirst();
		}
	}

	@Override
	public boolean isAfk()
	{
		return afkTime!=0;
	}

	@Override
	public void setAfkFlag(final boolean truefalse)
	{
		if((afkTime!=0)==truefalse)
			return;
		if(truefalse)
		{
			afkTime=System.currentTimeMillis();
			outputFormatter.println("\n\rYou are now listed as AFK.");
		}
		else
		{
			afkMessage=null;
			outputFormatter.println("\n\rYou are no longer AFK.");
			final MOB mob=this.mob;
			final PlayerStats pStats=(mob==null)?null:mob.playerStats();
			if((pStats != null)&&(mob!=null))
			{
				final int tells=pStats.queryTellStack(null, mob.Name(), Long.valueOf(afkTime)).size();
				final int gtells=pStats.queryGTellStack(null, mob.Name(), Long.valueOf(afkTime)).size();
				if((tells>0)||(gtells>0))
				{
					final StringBuilder missedStr = new StringBuilder("You missed: ");
					if(tells > 0)
						missedStr.append(tells).append(" tell(s), ");
					if(gtells > 0)
						missedStr.append(gtells).append(" gtell(s), ");
					mob.tell(missedStr.substring(0,missedStr.length()-2)+".");
				}
			}
			afkTime=0;
		}
	}

	@Override
	public String getAfkMessage()
	{
		if(mob==null)
			return "";
		if((afkMessage==null)||(CMStrings.removeColors(afkMessage).trim().length()==0))
			return mob.name()+" is AFK at the moment.";
		return afkMessage;
	}

	@Override
	public void setAFKMessage(final String str)
	{
		afkMessage = str;
	}

	protected void errorOut(final Exception t)
	{
		Log.errOut(t);
		CMLib.sessions().remove(this);
		setKillFlag(true);
	}

	@Override
	public boolean isLockedUpWriting()
	{
		return outputHandler.isLockedUpWriting();
	}

	public final void rawBytesOut(final byte[] bytes) throws IOException
	{
		if (outputHandler != null) {
			outputHandler.rawBytesOut(bytes);
		} else {
			// TODO: Consider a no-op behavior or logging.
			throw new IOException("No IOHandler configured for output");
		}
	}

	@Override
	public void snoopSupportPrint(final String msg, final boolean noCache)
	{
		try
		{
			if((snoops.size()>0)&&(snoopSuspensionStack<=0))
			{
				String msgColored;
				final String preFix=CMLib.coffeeFilter().colorOnlyFilter("^Z"+((mob==null)?"?":mob.Name())+":^N ",this);
				final int crCheck=msg.indexOf('\n');
				if((crCheck>=0)&&(crCheck<msg.length()-2))
				{
					final StringBuffer buf=new StringBuffer(msg);
					for(int i=buf.length()-1;i>=0;i--)
					{
						if((buf.charAt(i)=='\n')&&(i<buf.length()-2)&&(buf.charAt(i+1)=='\r'))
							buf.insert(i+2, preFix);
					}
					msgColored=buf.toString();
				}
				else
					msgColored=msg;
				for(int s=0;s<snoops.size();s++)
					snoops.get(s).getOutputFormatter().onlyPrint(preFix+msgColored,noCache);
			}
		}
		catch (final IndexOutOfBoundsException x)
		{
		}
	}

	@Override
	public void setPromptFlag(final boolean truefalse)
	{
		needPrompt=truefalse;
		outputFormatter.setNeedPrompt(truefalse);
	}

	@Override
	public String prompt(final String Message, final String Default, final long maxTime)
		throws IOException
	{
		final String Msg=prompt(Message,maxTime).trim();
		if(Msg.equals(""))
			return Default;
		return Msg;
	}

	@Override
	public String prompt(final String Message, final String Default)
		throws IOException
	{
		final String Msg=prompt(Message,-1).trim();
		if(Msg.equals(""))
			return Default;
		return Msg;
	}

	@Override
	public void prompt(final InputCallback callBack)
	{
		if(callBack!=null)
		{
			lastWasPrompt.set(true);
			callBack.showPrompt();
			lastWasPrompt.set(true);
		}
		if(this.inputCallback!=null)
			this.inputCallback.timedOut();
		this.inputCallback=callBack;
	}

	@Override
	public String prompt(final String Message, final long maxTime)
			throws IOException
	{
		promptPrint(Message);
		final String input=blockingIn(maxTime, true);
		if(input==null)
			return "";
		if((input.length()>0)&&(input.charAt(input.length()-1)=='\\'))
			return input.substring(0,input.length()-1);
		return input;
	}

	@Override
	public void promptPrint(final String msg)
	{
		lastWasPrompt.set(true);
		outputFormatter.print(msg);
		if(promptSuffix.length>0)
		{
			try
			{
				rawBytesOut(promptSuffix);
			}
			catch (final IOException e)
			{
			}
		}
		final MOB mob=mob();
		if((!getClientTelnetMode(TELNET_SUPRESS_GO_AHEAD))
				&& (!killFlag)
				&& (mightSupportTelnetMode(TELNET_GA)
				||(mob==null)
				||(mob.isAttributeSet(MOB.Attrib.TELNET_GA))))
		{
			try
			{
				rawBytesOut(TELNETGABYTES);
			}
			catch (final Exception e)
			{
			}
		}
		lastWasPrompt.set(true);
	}

	@Override
	public String prompt(final String Message)
		throws IOException
	{
		promptPrint(Message);
		final String input=blockingIn(-1, true);
		if(input==null)
			return "";
		if((input.length()>0)&&(input.charAt(input.length()-1)=='\\'))
			return input.substring(0,input.length()-1);
		return input;
	}

	@Override
	public String[] getColorCodes()
	{
		if(clookup==null)
			clookup=CMLib.color().standardColorLookups();

		if(mob()==null)
			return clookup;
		PlayerStats pstats=mob().playerStats();
		if((mob.soulMate()!=null)&&(mob.soulMate().playerStats()!=null))
			pstats=mob.soulMate().playerStats();
		if(pstats==null)
			return clookup;

		if(!pstats.getColorStr().equals(lastColorStr))
		{
			lastColorStr=pstats.getColorStr();
			if(pstats.getColorStr().length()==0)
				clookup=CMLib.color().standardColorLookups();
			else
				clookup=CMLib.color().fixPlayerColorDefs(lastColorStr);
		}
		return clookup;
	}

	public void handleSubOption(final int optionCode, final char[] suboptionData, final int dataSize)
		throws IOException
	{
		switch(optionCode)
		{
		case TELNET_TERMTYPE:
			if(dataSize >= 1)
			{
				if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
					Log.debugOut("For suboption "+Session.TELNET_DESCS[optionCode]+", got code "+((int)suboptionData[0])+": "+new String(suboptionData, 1, dataSize - 1));
				if(suboptionData[0] == 0)
				{
					terminalType = new String(suboptionData, 1, dataSize - 1);
					if(terminalType.equalsIgnoreCase("ZMUD")
					||terminalType.equalsIgnoreCase("CMUD")
					||terminalType.equalsIgnoreCase("XTERM"))
					{
						if(mightSupportTelnetMode(TELNET_ECHO))
							telnetSupportSet.remove(Integer.valueOf(TELNET_ECHO));
						changeTelnetMode(rawout,TELNET_ECHO,false);
					}
					else
					if(terminalType.equals("ANSI"))
						changeTelnetMode(rawout,TELNET_ECHO,true);
					else
					if(terminalType.startsWith("GIVE-WINTIN.NET-A-CHANCE"))
					{
						outputFormatter.rawOut("\n\r\n\r**** Your MUD Client is Broken! Please use another!!****\n\r\n\r");
						CMLib.s_sleep(1000);
						rawout.close();
					}
					else
					if(terminalType.toLowerCase().startsWith("mushclient")&&(!CMSecurity.isDisabled(CMSecurity.DisFlag.MXP)))
					{
						negotiateTelnetMode(rawout,TELNET_MXP);
						mxpSupportSet.remove("+IMAGE");
						mxpSupportSet.remove("+IMAGE.URL");
						mxpSupportSet.add("-IMAGE.URL");
					}
					else
					if(terminalType.toLowerCase().equals("simplemu")
					||(terminalType.toLowerCase().startsWith("mudlet")))
					{
						if(CMParms.indexOf(this.promptSuffix, (byte)'\n')<0)
						{
							promptSuffix = Arrays.copyOf(promptSuffix, promptSuffix.length+1);
							promptSuffix[promptSuffix.length-1] = (byte)'\n';
						}
						if(CMParms.indexOf(this.promptSuffix, (byte)'\r')<0)
						{
							promptSuffix = Arrays.copyOf(promptSuffix, promptSuffix.length+1);
							promptSuffix[promptSuffix.length-1] = (byte)'\r';
						}
						if((!this.mightSupportTelnetMode(Session.TELNET_GA))
						&&(CMParms.indexOf(this.promptSuffix, Session.TELNETGABYTES)<0))
						{
							final int pos = promptSuffix.length;
							promptSuffix = Arrays.copyOf(promptSuffix, promptSuffix.length + Session.TELNETGABYTES.length);
							System.arraycopy(Session.TELNETGABYTES, 0, promptSuffix, pos, Session.TELNETGABYTES.length);
						}
					}
				}
				else
				if (suboptionData[0] == 1) // Request for data.
				{/* No idea how to handle this, ignore it for now. */
				}
			}
			break;
		case TELNET_NAWS:
			if (dataSize == 4)  // It should always be 4.
			{
				terminalWidth = ((suboptionData[0] << 8) | suboptionData[1])-2;
				terminalHeight = (suboptionData[2] << 8) | suboptionData[3];
				if(terminalWidth > CMStrings.SPACES.length())
					terminalWidth=CMStrings.SPACES.length();
				if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
					Log.debugOut("For suboption "+Session.TELNET_DESCS[optionCode]+", got: "+terminalWidth+"x"+terminalHeight);
			}
			break;
		case TELNET_MSDP:
			{
				final byte[] resp=CMLib.protocol().processMsdp(this, suboptionData, dataSize, this.msdpReportables);
				if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
					Log.debugOut("For suboption "+Session.TELNET_DESCS[optionCode]+", got "+dataSize+" bytes, sent "+((resp==null)?0:resp.length));
				if(resp!=null)
					rawBytesOut(resp);
			}
			break;
		case TELNET_GMCP:
			{
				final byte[] resp=CMLib.protocol().processGmcp(this, new String(suboptionData), gmcpSupports, msdpReportables);
				if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
				{
					Log.debugOut("For suboption "+Session.TELNET_DESCS[optionCode]+", got "+dataSize+" bytes, sent "+((resp==null)?0:resp.length));
					Log.debugOut(new String(suboptionData));
				}
				if(resp!=null)
					rawBytesOut(resp);
			}
			break;
		default:
			// Ignore it.
			break;
		}
	}

	public void handleEscape() throws IOException, InterruptedIOException
	{
		if(outputHandler == null)
			return;
		int c=readByte();
		if((char)c!='[')
			return;

		boolean quote=false;
		final StringBuffer escapeStr=new StringBuffer("");
		while(((c=readByte())!=-1)
		&&(!killFlag)
		&&((quote)||(!Character.isLetter((char)c))))
		{
			escapeStr.append((char)c);
			if(c=='"')
				quote=!quote;
		}
		if(c==-1)
			return;
		escapeStr.append((char)c);
		String esc=escapeStr.toString().trim();
		// at the moment, we only handle mxp escapes
		// everything else is effectively EATEN
		if(!esc.endsWith("z"))
			return;
		esc=esc.substring(0,esc.length()-1);
		if(!CMath.isNumber(esc))
			return;
		final int escNum=CMath.s_int(esc);
		// only LINE-based mxp escape sequences are respected
		if(escNum>3)
			return;
		sock.setSoTimeout(30000);
		final StringBuffer line=new StringBuffer("");
		while(((c=readByte())!=-1)&&(!killFlag))
		{
			if(c=='\n')
				break;
			line.append((char)c);
		}
		sock.setSoTimeout(SOTIMEOUT);
		String l=line.toString().toUpperCase().trim();
		// now we have the line, so parse out tags -- only tags matter!
		while(l.length()>0)
		{
			final int tagStart=l.indexOf('<');
			if(tagStart<0)
				return;
			final int tagEnd=l.indexOf('>');
			if(tagEnd<0)
				return;
			String tag=l.substring(tagStart+1,tagEnd).trim();
			l=l.substring(tagEnd+1).trim();
			// now we have a tag, and its parameters (space delimited)
			final List<String> parts=CMParms.parseSpaces(tag,true);
			if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
				Log.debugOut("Got secure MXP tag: "+tag);
			if(parts.size()>1)
			{
				tag=parts.get(0);
				if(tag.equals("VERSION"))
				{
					for(int p=1;p<parts.size();p++)
					{
						final String pp=parts.get(p);
						final int x=pp.indexOf('=');
						if(x<0)
							continue;
						mxpVersionInfo.remove(pp.substring(0,x).trim());
						mxpVersionInfo.put(pp.substring(0,x).trim(),pp.substring(x+1).trim());
					}
				}
				else
				if(tag.equals("SUPPORTS"))
				{
					for(int p=1;p<parts.size();p++)
					{
						final String s=parts.get(p).toUpperCase();
						final int x=s.indexOf('.');
						if(s.startsWith("+"))
						{
							if((terminalType == null)
							||(!terminalType.toLowerCase().startsWith("mushclient"))
							||(s.indexOf("IMAGE")<0))
							{
								mxpSupportSet.add(s);
								if(x>0)
									mxpSupportSet.add(s.substring(0, x));
							}
						}
						else
						if(s.startsWith("-"))
						{
							mxpSupportSet.add(s);
							mxpSupportSet.remove("+"+s.substring(1));
						}
					}
				}
				else
				if(tag.equals("SHUTDOWN"))
				{
					final MOB M=CMLib.players().getLoadPlayer(parts.get(1));
					if((M!=null)
					&&(M.playerStats().matchesPassword(parts.get(2)))
					&&(CMSecurity.isASysOp(M)))
					{
						final boolean keepDown=parts.size()>3?CMath.s_bool(parts.get(3)):true;
						final String externalCmd=(parts.size()>4)?CMParms.combine(parts,4):null;
						final Vector<String> cmd=new XVector<String>("SHUTDOWN","NOPROMPT");
						if(!keepDown)
						{
							cmd.add("RESTART");
							if((externalCmd!=null)&&(externalCmd.length()>0))
								cmd.add(externalCmd);
						}
						final Command C=CMClass.getCommand("Shutdown");
						l="";
						setKillFlag(true);
						outputFormatter.rawCharsOut("\n\n\033[1z<Executing Shutdown...\n\n".toCharArray());
						M.setSession(this);
						if(C!=null)
							C.execute(M,cmd,0);
					}
				}
			}
		}
	}

	public void handleIAC()
		throws IOException, InterruptedIOException
	{
		if(outputHandler==null)
			return;
		lastIACIn=System.currentTimeMillis();
		int c=readByte();
		if(c>255)
			c=c&0xff;

		switch(c)
		{
		case TELNET_IAC:
			bNextByteIs255=true;
			break;
		case TELNET_SB:
		{
			CharArrayWriter subOptionStream=new CharArrayWriter();
			final int subOptionCode = readByte();
			int last = 0;
			if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
				Log.debugOut("Reading sub-option "+subOptionCode);
			final long expire=System.currentTimeMillis() + 100;
			while((System.currentTimeMillis()<expire)
			&&(!killFlag))
			{
				try
				{
					last = readByte();
					if(last != -1)
					{
						if(subOptionStream.size()>1024*1024*5)
						{
							setKillFlag(true);
							return;
						}
						else
						if (last == TELNET_IAC)
						{
							last = readByte();
							if (last == TELNET_IAC)
								subOptionStream.write(TELNET_IAC);
							else
							if (last == TELNET_SE)
								break;
						}
						else
							subOptionStream.write((char)last);
					}
				}
				catch(final IOException e)
				{
				}
			}
			final char[] subOptionData=subOptionStream.toCharArray();
			subOptionStream=null;
			handleSubOption(subOptionCode, subOptionData, subOptionData.length);
			break;
		}
		case TELNET_DO:
		{
			final int last=readByte();
			setClientTelnetMode(last,true);
			if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
				Log.debugOut("Got DO "+Session.TELNET_DESCS[last]);
			if((terminalType.equalsIgnoreCase("zmud")||terminalType.equalsIgnoreCase("cmud"))&&(last==Session.TELNET_ECHO))
				setClientTelnetMode(Session.TELNET_ECHO,false);
			if(last==TELNET_TERMTYPE)
				negotiateTelnetMode(rawout,TELNET_TERMTYPE);
			if((last==TELNET_COMPRESS2)&&(getServerTelnetMode(last)))
			{
				setClientTelnetMode(last,true);
				if(connectionComplete)
				{
					prompt(new TickingCallback(250)
					{
						@Override
						public boolean tick(final int counter)
						{
							try
							{
								if((outputHandler==null)||(killFlag))
									return false;
								switch(counter)
								{
								case 3:
								{
									if(getClientTelnetMode(TELNET_COMPRESS2))
									{
										negotiateTelnetMode(rawout,TELNET_COMPRESS2);
										final ZOutputStream zOut=new ZOutputStream(rawout, JZlib.Z_DEFAULT_COMPRESSION);
										rawout=zOut;
										outputHandler.resetByteStream(rawout);
										zOut.setFlushMode(JZlib.Z_SYNC_FLUSH);
										if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
											Log.debugOut("MCCP compression started");
									}
									break;
								}
								case 10:
									return false;
								default:
									break;
								}
								return true;
							}
							catch(final IOException e)
							{
								if(e.getMessage()==null)
									Log.errOut(e);
								else
									Log.errOut(e.getMessage());
							}
							return false;
						}
					});
				}
			}
			else
			if(!mightSupportTelnetMode(last))
				changeTelnetMode(last,false);
			else
			{
				if(!getServerTelnetMode(last))
					changeTelnetMode(last,true);
				if(last == TELNET_MSSP)
				{
					final ByteArrayOutputStream buf=new ByteArrayOutputStream();
					buf.write(Session.TELNET_IAC);buf.write(Session.TELNET_SB);buf.write(Session.TELNET_MSSP);
					final Map<String,Object> pkg = CMLib.protocol().getMSSPPackage();
					for(final String key : pkg.keySet())
					{
						final Object o = pkg.get(key);
						buf.write(1);
						buf.write(key.getBytes("UTF-8"));
						if(o instanceof String[])
						{
							final String[] os = (String[])o;
							for(final String s : os)
							{
								buf.write(2);
								buf.write(s.getBytes("UTF-8"));
							}
						}
						else
						{
							buf.write(2);
							buf.write(o.toString().getBytes("UTF-8"));
						}
					}
					buf.write((char)Session.TELNET_IAC);buf.write((char)Session.TELNET_SE);
					try
					{
						rawBytesOut(buf.toByteArray());
					}
					catch (final IOException e)
					{
					}
				}
			}
			if(serverTelnetCodes[TELNET_LOGOUT])
				setKillFlag(true);
			break;
		}
		case TELNET_DONT:
		{
			final int last=readByte();
			if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
				Log.debugOut("Got DONT "+Session.TELNET_DESCS[last]);
			setClientTelnetMode(last,false);
			if((last==TELNET_COMPRESS2)&&(getServerTelnetMode(last)))
			{
				setClientTelnetMode(last,false);
				rawout=new BufferedOutputStream(sock.getOutputStream());
				outputHandler.resetByteStream(rawout);
			}
			if((mightSupportTelnetMode(last)&&(getServerTelnetMode(last))))
				changeTelnetMode(last,false);
			break;
		}
		case TELNET_WILL:
		{
			final int last=readByte();
			if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
				Log.debugOut("Got WILL "+Session.TELNET_DESCS[last]);
			setClientTelnetMode(last,true);
			if((terminalType.equalsIgnoreCase("zmud")||terminalType.equalsIgnoreCase("cmud"))&&(last==Session.TELNET_ECHO))
				setClientTelnetMode(Session.TELNET_ECHO,false);
			if(!mightSupportTelnetMode(last))
				changeTelnetModeBackwards(last,false);
			else
			if(!getServerTelnetMode(last))
				changeTelnetModeBackwards(last,true);
			if(serverTelnetCodes[TELNET_LOGOUT])
				setKillFlag(true);
			break;
		}
		case TELNET_WONT:
		{
			final int last=readByte();
			if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
				Log.debugOut("Got WONT "+Session.TELNET_DESCS[last]);
			setClientTelnetMode(last,false);
			if((mightSupportTelnetMode(last))&&(getServerTelnetMode(last)))
				changeTelnetModeBackwards(last,false);
			break;
		}
		case TELNET_AYT:
			outputFormatter.rawCharsOut(" \b");
			break;
		default:
			return;
		}
	}

	public int readByte() throws IOException
	{
		return inputHandler.readByte(bNextByteIs255, fakeInput);
	}

	public int readChar() throws IOException
	{
		int c = inputHandler.readChar(bNextByteIs255, fakeInput);
		if (fakeInput != null && fakeInput.length() == 0) {
			fakeInput = null;
		}
		return c;
	}

	@Override
	public char hotkey(final long maxWait)
	{
		if(outputHandler==null)
			return '\0';
		input=new StringBuffer("");
		final long start=System.currentTimeMillis();
		try
		{
			suspendCommandLine=true;
			char c='\0';
			while((!killFlag)
			&&((maxWait<=0)||((System.currentTimeMillis()-start)<maxWait)))
			{
				c=(char)nonBlockingIn(false);
				if((c==(char)0)||(c==(char)1)||(c==(char)-1))
					continue;
				return c;
			}
			suspendCommandLine=false;
			if((maxWait>0)&&((System.currentTimeMillis()-start)>=maxWait))
				throw new java.io.InterruptedIOException(TIMEOUT_MSG);
		}
		catch (final java.io.IOException e)
		{
		}
		return '\0';
	}

	protected int nonBlockingIn(final boolean appendInputFlag)
	throws IOException
	{
		try
		{
			int c=readChar();
			if(c<0)
				throw new IOException("reset by peer");
			else
			if((c==TELNET_IAC)||((c&0xff)==TELNET_IAC))
				handleIAC();
			else
			if(c=='\033')
				handleEscape();
			else
			{
				boolean rv = false;
				switch (c)
				{
					case 0:
					{
						c=-1;
						lastWasCR = false;
						lastWasLF = false;
					}
					break;
					case 10:
					{
						c=-1;
						if(!lastWasCR)
						{
							lastWasLF = true;
							rv = true;
						}
						else
							lastWasLF = false;
						lastWasCR = false;
						if (getClientTelnetMode(TELNET_ECHO))
							outputFormatter.rawCharsOut(""+(char)13+(char)10);  // CR
						break;
					}
					case 13:
					{
						c=-1;
						if(!lastWasLF)
						{
							lastWasCR = true;
							rv = true;
						}
						else
							lastWasCR = false;
						lastWasLF = false;
						if (getClientTelnetMode(TELNET_ECHO))
							outputFormatter.rawCharsOut(""+(char)13+(char)10);  // CR
						break;
					}
					case 27:
					{
						lastWasCR = false;
						lastWasLF = false;
						// don't let them enter ANSI escape sequences...
						c = -1;
						break;
					}
					default:
					{
						if(((c>>8)&0xff)>241)
							c=-1;
						lastWasCR = false;
						lastWasLF = false;
						break;
					}
				}

				if(c>0)
				{
					lastKeystroke=System.currentTimeMillis();
					if(appendInputFlag)
						input.append((char)c);
					if (getClientTelnetMode(TELNET_ECHO))
						outputFormatter.rawCharsOut((char)c);
					if(!appendInputFlag)
						return c;
				}
				if(rv)
				{
					lastWasPrompt.set(false);
					return 0;
				}
			}
		}
		catch(final InterruptedIOException e)
		{
			return -1;
		}
		return 1;
	}

	protected void clearInputBuffer(final long maxTime)
	{
		try
		{
			blockingIn(maxTime, true);
		}
		catch(final Exception e)
		{
		}
	}

	@Override
	public String blockingIn(final long maxTime, final boolean filter)
		throws IOException
	{
		if(outputHandler==null)
			return "";
		if(runThread != Thread.currentThread()) {
			// FIXME
			if(runThread != Thread.currentThread()) {
				Log.warnOut("DefaultSession", "blockingIn: Method called on non-runThread. This may cause unexpected behavior or threading issues.");
				return "";
			}
			return "";
		}
		this.input.setLength(0);
		final long start=System.currentTimeMillis();
		final long timeoutTime= (maxTime<=0) ? Long.MAX_VALUE : (start + maxTime);
		long nextPingAtTime=start + PINGTIMEOUT;
		try
		{
			suspendCommandLine=true;
			long now;
			long lastC;
			StringBuilder inStr = null;
			while((!killFlag)
			&&((now=System.currentTimeMillis())<timeoutTime))
			{
				if((lastC=nonBlockingIn(true))==0)
				{
					inStr=new StringBuilder(input);
					this.input.setLength(0);
					if(sessionMCPCheck(inStr))
						break;
				}
				else
				if(lastC == -1)
				{
					if(now > nextPingAtTime)
					{
						outputFormatter.rawCharsOut(PINGCHARS);
						nextPingAtTime=now +PINGTIMEOUT;
					}
					CMLib.s_sleep(100); // if they entered nothing, make sure we dont do a busy poll
				}
				else
					nextPingAtTime=now+PINGTIMEOUT;
			}
			if(inStr == null)
				inStr = new StringBuilder();
			suspendCommandLine=false;
			if(System.currentTimeMillis()>=timeoutTime)
				throw new java.io.InterruptedIOException(TIMEOUT_MSG);

			final String str;
			if(filter)
			{
				final boolean doMxp = CMSecurity.isAllowed(mob,(mob!=null)?mob.location():null,CMSecurity.SecFlag.MXPTAGS);
				final boolean isArchon = CMSecurity.isASysOp(mob);
				str=CMLib.coffeeFilter().simpleInFilter(inStr,doMxp, isArchon);
			}
			else
				str=inStr.toString();
			if(str==null)
				return null;
			snoopSupportPrint(str+"\n\r",true);
			if(debugStrInput)
				Log.sysOut("INPUT: "+(mob==null?"":mob.Name())+": '"+inStr.toString()+"'");
			final MOB mob=this.mob;
			if((mob != null)&&(mob.isAttributeSet(Attrib.NOREPROMPT)))
				setPromptFlag(true);
			return str;
		}
		finally
		{
			suspendCommandLine=false;
		}
	}

	public String blockingIn() throws IOException
	{
		return blockingIn(-1, true);
	}

	protected boolean sessionMCPCheck(final StringBuilder inStr)
	{
		if((inStr.length()>3)
		&&(inStr.charAt(0)=='#')
		&&(inStr.charAt(1)=='$'))
		{
			if(inStr.substring(0, 3).equals("#$#"))
			{
				if(debugStrInput)
					Log.sysOut("INPUT: "+(mob==null?"":mob.Name())+": '"+inStr.toString()+"'");
				if(CMLib.protocol().mcp(this,inStr,mcpKey,mcpSupported,mcpKeyPairs))
					return false;
			}
			else
			if(inStr.substring(0, 3).equals("#$\""))
				inStr.delete(0, 3);
		}
		return true;
	}

	@Override
	public String readlineContinue() throws IOException, SocketException
	{
		if(outputHandler==null)
			return "";
		int code=-1;
		while(!killFlag)
		{
			synchronized(sock)
			{
				if(sock.isClosed() || (!sock.isConnected()))
				{
					if(mob()!=null)
						stopSession(true,CMLib.flags().isInTheGame(mob(),true),false);
					else
						setKillFlag(true);
					return null;
				}
			}
			code=nonBlockingIn(true);
			if(code==1)
				continue;
			if(code==0)
				break;
			if(code==-1)
				return null;
		}

		final StringBuilder inStr=new StringBuilder(input);
		input.setLength(0);
		if(!sessionMCPCheck(inStr))
			return null;
		final boolean doMxp = CMSecurity.isAllowed(mob,(mob!=null)?mob.location():null,CMSecurity.SecFlag.MXPTAGS);
		final boolean isArchon = CMSecurity.isASysOp(mob);
		final String str=CMLib.coffeeFilter().simpleInFilter(inStr,doMxp, isArchon);
		if(str==null)
			return null;
		snoopSupportPrint(str+"\n\r",true);
		if(debugStrInput)
			Log.sysOut("INPUT: "+(mob==null?"":mob.Name())+": '"+inStr.toString()+"'");
		final MOB mob=this.mob;
		if((mob != null)&&(mob.isAttributeSet(Attrib.NOREPROMPT)))
			setPromptFlag(true);
		return str;
	}

	@Override
	public boolean confirm(final String Message, String Default, final long maxTime) throws IOException
	{
		if(Default.toUpperCase().startsWith("T"))
			Default="Y";
		final String YN=choose(Message,"YN",Default,maxTime);
		if(YN.equals("Y"))
			return true;
		return false;
	}

	@Override
	public boolean confirm(final String Message, String Default) throws IOException
	{
		if(Default.toUpperCase().startsWith("T"))
			Default="Y";
		final String YN=choose(Message,"YN",Default,-1);
		if(YN.equals("Y"))
			return true;
		return false;
	}

	@Override
	public String choose(final String Message, final String Choices, final String Default) throws IOException
	{
		return choose(Message,Choices,Default,-1,null);
	}

	@Override
	public String choose(final String Message, final String Choices, final String Default, final long maxTime) throws IOException
	{
		return choose(Message,Choices,Default,maxTime,null);
	}

	@Override
	public String choose(final String promptMsg, final String choices, final String def, final long maxTime, final List<String> paramsOut) throws IOException
	{
		String YN="";
		String rest=null;
		final List<String> choiceList;
		final boolean oneChar=choices.indexOf(',')<0;
		if(!oneChar)
			choiceList=CMParms.parseCommas(choices,true);
		else
		{
			choiceList=new ArrayList<String>();
			for(final char c : choices.toCharArray())
				choiceList.add(""+c);
		}
		while((YN.equals("")||(!CMParms.containsIgnoreCase(choiceList, YN)))
		&&(!killFlag))
		{
			promptPrint(promptMsg);
			YN=blockingIn(maxTime, true);
			if(YN==null)
				return def.toUpperCase();
			YN=YN.trim();
			if(YN.equals(""))
				return def.toUpperCase();
			if((YN.length()>1)
			&&(oneChar))
			{
				if(paramsOut!=null)
					rest=YN.substring(1).trim();
				YN=YN.substring(0,1).toUpperCase();
			}
			else
			if(oneChar)
				YN=YN.toUpperCase();
		}
		if((rest!=null)&&(paramsOut!=null)&&(rest.length()>0))
			paramsOut.addAll(CMParms.cleanParameterList(rest));
		return YN;
	}

	@Override
	public void stopSession(final boolean removeMOB, final boolean dropSession, final boolean killThread)
	{
		setKillFlag(true);
		setStatus(SessionStatus.LOGOUT5);
		if(dropSession)
		{
			preLogout(mob);
		}
		final MOB M = mob();
		if(removeMOB && (M!=null))
		{
			if((CMSecurity.isDisabled(CMSecurity.DisFlag.LOGOUTS)
				||(!CMLib.masking().maskCheck(CMProps.getVar(CMProps.Str.LOGOUTMASK), mob(), true)))
			&&(!CMSecurity.isASysOp(mob())))
			{ /* do nothing right now */ }
			else
				M.removeFromGame(true, dropSession);
		}
		if(dropSession)
			logoutFinal();
		if(killThread)
		{
			// this is a really really bad idea, because any other sessions
			// on the same thread get killed also!
			Thread killThisThread=null;
			synchronized(this)
			{
				if(runThread==Thread.currentThread())
					setKillFlag(true);
				else
				if(runThread!=null)
					killThisThread=runThread;
			}
			if(killThisThread!=null)
				killThisThread.interrupt();
			killThisThread=writeThread;
			if(killThisThread!=null)
				killThisThread.interrupt();
		}
	}

	public void showPrompt()
	{
		promptLastShown=System.currentTimeMillis();
		final MOB mob=mob();
		if(mob==null)
			return;
		final StringBuilder buf=new StringBuilder("");
		if(mob.playerStats()==null)
			return;
		if(getClientTelnetMode(Session.TELNET_MXP))
		{
			buf.append("^<!ENTITY Hp '").append(mob.curState().getHitPoints())
				.append("'^>^<!ENTITY MaxHp '").append(mob.maxState().getHitPoints())
				.append("'^>^<!ENTITY Mana '").append(mob.curState().getMana())
				.append("'^>^<!ENTITY MaxMana '").append(mob.maxState().getMana())
				.append("'^>^<!ENTITY Move '").append(mob.curState().getMovement())
				.append("'^>^<!ENTITY MaxMove '").append(mob.maxState().getMovement())
				.append("'^>^<!ENTITY Exp '").append(mob.getExperience())
				.append("'^>^<!ENTITY ExpNeed '").append(mob.getExpNeededLevel())
				.append("'^>\n\r");
		}
		buf.append(CMLib.utensils().buildPrompt(mob, mob.playerStats().getPrompt()));
		promptPrint("^<Prompt^>"+buf.toString()+"^</Prompt^>^.^N");
	}

	protected void closeSocks(final String finalMsg)
	{
		final Socket sock=this.sock;
		if((sock!=null) && (!socketShutdownLock.get()))
		{
			socketShutdownLock.set(true);
			try
			{
				Log.sysOut("Disconnect: "+finalMsg+getAddress()+" ("+CMLib.time().date2SmartEllapsedTime(getMillisOnline(),true)+")");
				outputFormatter.shutdown();
				setStatus(SessionStatus.LOGOUT7);
				inputHandler.shutdown();
				setStatus(SessionStatus.LOGOUT8);
				outputHandler.shutdown();
				setStatus(SessionStatus.LOGOUT9);
				sock.shutdownInput();
				setStatus(SessionStatus.LOGOUT10);
				sock.shutdownOutput();
				setStatus(SessionStatus.LOGOUT11);
				sock.close();
				setStatus(SessionStatus.LOGOUT12);
			}
			catch(final IOException | IndexOutOfBoundsException e)
			{
				Log.warnOut("DefaultSession", "closeSocks: Error during session shutdown: " + e.getMessage());
			} finally
			{
				this.outputHandler = null;
				this.inputHandler = null;
				this.outputFormatter = null;
				this.sock=null;
				this.socketShutdownLock.set(false);
			}
		}
	}

	@Override
	public String getAddress()
	{
		try
		{
			return sock.getInetAddress().getHostAddress();
		}
		catch (final Exception e)
		{
			return "Unknown (Excpt "+e.getMessage() + ")";
		}
	}

	private void preLogout(final MOB M)
	{
		if(M==null)
			return;
		synchronized(loggingOutObj)
		{
			if(loggingOutObj[0])
				return;
			try
			{
				loggingOutObj[0]=true;
				final MOB mob=this.mob;
				final boolean inTheGame=CMLib.flags().isInTheGame(M,true);
				if(inTheGame
				&& (M.location()!=null)
				&&(mob!=null)
				&&((!CMProps.getBoolVar(CMProps.Bool.MUDSHUTTINGDOWN))
					||(CMLib.sessions().numSessions()>1)))
				{
					final List<Room> rooms=new ArrayList<Room>(1);
					rooms.add(M.location());
					for(final MOB M2 : M.getGroupMembers(new HashSet<MOB>()))
					{
						if((M2.location()!=null)&&(!rooms.contains(M2.location())))
							rooms.add(M2.location());
					}
					final CMMsg quitMsg=CMClass.getMsg(mob, CMMsg.MSG_QUIT, null);
					for(final Room R : rooms)
					{
						try
						{
							R.send(M, quitMsg);
						}
						catch (final Throwable t)
						{ /* and eat it */
							Log.errOut(t.getMessage());
						}
					}
				}

				while((getLastPKFight()>0)
				&&((System.currentTimeMillis()-getLastPKFight())<(2*60*1000))
				&&(mob!=null))
					CMLib.s_sleep(1000);
				String name=M.Name();
				if(name.trim().length()==0)
					name="Unknown";
				if((M.isInCombat())&&(M.location()!=null))
				{
					CMLib.commands().postFlee(mob,"NOWHERE");
					M.makePeace(false);
				}
				if(!CMLib.flags().isCloaked(M))
				{
					final List<String> channels=CMLib.channels().getFlaggedChannelNames(ChannelsLibrary.ChannelFlag.LOGOFFS, M);
					for(int i=0;i<channels.size();i++)
						CMLib.commands().postChannel(channels.get(i),M.clans(),L("@x1 has logged out",name),true);
				}
				if(!M.isAttributeSet(Attrib.PRIVACY))
					CMLib.login().notifyFriends(M,L("^X@x1 has logged off.^.^?",M.Name()));

				// the player quit message!
				CMLib.threads().executeRunnable(groupName,new LoginLogoutThread(M,CMMsg.MSG_QUIT));
				if(M.playerStats()!=null)
					M.playerStats().setLastDateTime(System.currentTimeMillis());
				Log.sysOut("Logout: "+name+" ("+CMLib.time().date2SmartEllapsedTime(System.currentTimeMillis()-userLoginTime,true)+")");
				if(inTheGame)
					CMLib.database().DBUpdateFollowers(M);
			}
			finally
			{
				loggingOutObj[0]=false;
			}
		}
	}

	@Override
	public SessionStatus getStatus()
	{
		return status;
	}

	@Override
	public boolean isWaitingForInput()
	{
		return (inputCallback!=null);
	}

	@Override
	public void logout(final boolean removeMOB)
	{
		final MOB M = this.mob;
		if((M==null)||(M.playerStats()==null))
			stopSession(false,false,false);
		else
		{
			preLogout(M);
			if(removeMOB)
				M.removeFromGame(true, false);
			M.setSession(null);
			this.mob=null;
		}
	}

	@Override
	public boolean isRunning()
	{
		return runThread!=null;
	}

	@Override
	public boolean isPendingLogin(final String otherLoginName)
	{
		switch(status)
		{
			case LOGIN:
			case LOGIN2:
			case HANDSHAKE_OPEN:
			case HANDSHAKE_MCCP:
			case HANDSHAKE_MXP:
			case HANDSHAKE_MXPPAUSE:
			case HANDSHAKE_DONE:
				break;
			default:
				return false;
		}
		if(loginSession==null)
			return false;
		final String myLogin=loginSession.login();
		if((otherLoginName==null)||(myLogin==null))
			return false;
		return otherLoginName.equalsIgnoreCase(myLogin);
	}

	protected void doProtocolPings()
	{
		if(getClientTelnetMode(TELNET_MSDP))
		{
			final byte[] msdpPingBuf=CMLib.protocol().pingMsdp(this, msdpReportables);
			if(msdpPingBuf!=null)
			{
				try
				{
					rawBytesOut(msdpPingBuf);
				}
				catch (final IOException e)
				{
				}
				if(CMSecurity.isDebugging(CMSecurity.DbgFlag.TELNET))
					Log.debugOut("MSDP Reported: "+msdpPingBuf.length+" bytes");
			}
		}
		if(getClientTelnetMode(TELNET_GMCP))
		{
			final byte[] gmcpPingBuf=CMLib.protocol().pingGmcp(this, gmcpPings, gmcpSupports, msdpReportables);
			if(gmcpPingBuf!=null)
			{
				try
				{
					rawBytesOut(gmcpPingBuf);
				}
				catch (final IOException e)
				{
				}
			}
		}
	}

	@Override
	public void run()
	{
		synchronized(this)
		{
			if(runThread!=null)
			{
				// one at a time, thank you.
				return;
			}
			runThread=Thread.currentThread();
		}

		activeMillis=System.currentTimeMillis();
		if((activeMillis>=nextMsdpPing)&&(connectionComplete))
		{
			nextMsdpPing=activeMillis+MSDPPINGINTERVAL;
			doProtocolPings();
		}

		try
		{
			if(killFlag)
				setStatus(SessionStatus.LOGOUT);
			final InputCallback callBack=this.inputCallback;
			if(callBack!=null)
			{
				try
				{
					setInputLoopTime(); // update the input loop time so we don't get suspicious
					final String input=readlineContinue();
					if(input != null)
					{
						callBack.setInput(input);
						if(!callBack.waitForInput())
						{
							CMLib.threads().executeRunnable(groupName,new Runnable()
							{
								@Override
								public void run()
								{
									try
									{
										callBack.callBack();
									}
									catch(final Throwable t)
									{
										Log.errOut(t);
									}
								}
							});
						}
					}
					else
					if(callBack.isTimedOut())
					{
						callBack.timedOut();
					}
				}
				catch(final Exception e)
				{

				}
				if(!callBack.waitForInput())
					inputCallback=null;
			}
			else
			switch(status)
			{
			case IDLE:
			case HANDSHAKE_OPEN:
			case HANDSHAKE_MCCP:
			case HANDSHAKE_MXP:
			case HANDSHAKE_MXPPAUSE:
			case HANDSHAKE_DONE:
				break;
			case MAINLOOP:
				mainLoop();
				break;
			case LOGIN:
			case ACCOUNT_MENU:
			case CHARCREATE:
			case LOGIN2:
				loginSystem();
				break;
			case LOGOUT:
			case LOGOUT1:
			case LOGOUT2:
			case LOGOUT3:
			case LOGOUT4:
			case LOGOUT5:
			case LOGOUT6:
			case LOGOUT7:
			case LOGOUT8:
			case LOGOUT9:
			case LOGOUT10:
			case LOGOUT11:
			case LOGOUT12:
			case LOGOUTFINAL:
			{
				inputCallback=null;
				preLogout(mob);
				logoutFinal();
				break;
			}
			}
		}
		catch(final Throwable t)
		{
			Log.errOut(t);
		}
		finally
		{
			synchronized(this)
			{
				runThread=null;
			}
			activeMillis=0;
		}
	}

	@Override
	public boolean autoLogin(String name, final String password)
	{
		if(name==null)
		{
			if(password!=null)
				return false;
			this.loginSession=null;
			setStatus(SessionStatus.LOGIN);
			return true;
		}
		else
		if(password==null)
			return false;
		name = CMStrings.capitalizeAndLower(name);
		final MOB mob=CMLib.players().getLoadPlayer(name);
		if((mob==null)||(mob.playerStats()==null))
			return false;
		if(!CMLib.encoder().passwordCheck(password, mob.playerStats().getPasswordStr()))
			return false;
		try
		{
			setMob(mob);
			if(CMLib.login().completePlayerLogin(this,false) == CharCreationLibrary.LoginResult.NORMAL_LOGIN)
			{
				this.nonBlockingIn(false);
			}
			else
			{
				setMob(null);
				return false;
			}
		}
		catch(final Exception e)
		{
		}
		return setLoggedInState(LoginResult.NORMAL_LOGIN);
	}

	protected boolean setLoggedInState(final LoginResult loginResult)
	{
		setStatus(SessionStatus.LOGIN2);
		final MOB mob=this.mob;
		if((mob!=null)&&(mob.playerStats()!=null))
			acct=mob.playerStats().getAccount();
		if((!killFlag)&&((mob!=null)))
		{
			if(mob.playerStats()!=null)
				CMLib.threads().suspendResumeRecurse(mob, false, false);
			userLoginTime=System.currentTimeMillis();
			final String ansiStr;
			if((mob!=null)
			&&(mob.isAttributeSet(MOB.Attrib.ANSI)&&getClientTelnetMode(Session.TELNET_ANSI)))
			{
				if((mob!=null)&&(!mob.isAttributeSet(MOB.Attrib.ANSI16)))
					ansiStr = " ANSI";
				else
					ansiStr = " ANSI-16";
			}
			else
				ansiStr="";
			final StringBuilder loginMsg=new StringBuilder("");
			if(mob != null)
				loginMsg.append(getAddress()).append(" "+terminalType)
				.append(((mob.isAttributeSet(MOB.Attrib.MXP)&&getClientTelnetMode(Session.TELNET_MXP)))?" MXP":"")
				.append(getClientTelnetMode(Session.TELNET_MSDP)?" MSDP":"")
				.append(getClientTelnetMode(Session.TELNET_ATCP)?" ATCP":"")
				.append(getClientTelnetMode(Session.TELNET_GMCP)?" GMCP":"")
				.append((getClientTelnetMode(Session.TELNET_COMPRESS)||getClientTelnetMode(Session.TELNET_COMPRESS2))?" CMP":"")
				.append(ansiStr)
				.append(", character login: "+mob.Name());
			Log.sysOut(loginMsg.toString());
			if(loginResult != CharCreationLibrary.LoginResult.NO_LOGIN)
			{
				final CMMsg msg = CMClass.getMsg(mob,null,CMMsg.MSG_LOGIN,null);
				if(!CMLib.map().sendGlobalMessage(mob,CMMsg.TYP_LOGIN,msg))
					setKillFlag(true);
				else
				if(mob != null)
					CMLib.commands().monitorGlobalMessage(mob.location(), msg);
			}
		}

		setPromptFlag(true);
		if((!killFlag)&&(mob!=null))
		{
			setStatus(SessionStatus.MAINLOOP);
			return true;
		}
		return false;
	}

	public void loginSystem()
	{
		try
		{
			if((loginSession==null)||(loginSession.reset()))
			{
				loginSession = CMLib.login().createLoginSession(this);
				setStatus(SessionStatus.LOGIN);
			}
			else
			if(!loginSession.skipInputThisTime())
			{
				final String lastInput = loginSession.acceptInput(this);
				if(lastInput==null)
				{
					if((System.currentTimeMillis()-outputHandler.getLastWriteTime())>PINGTIMEOUT)
						outputFormatter.rawCharsOut(PINGCHARS);
					return;
				}
				if(!killFlag)
					setInputLoopTime();
			}
			if(loginSession == null)
				setKillFlag(true);
			else
			if(!killFlag)
			{
				final CharCreationLibrary.LoginResult loginResult=loginSession.loginSystem(this);
				switch(loginResult)
				{
				case INPUT_REQUIRED:
					return;
				case NO_LOGIN:
				{
					mob=null;
					setStatus(SessionStatus.LOGIN);
					return;
				}
				case NORMAL_LOGIN:
				{
					if(setLoggedInState(loginResult))
						return;
				}
				}
				setStatus(SessionStatus.LOGIN);
				return;
			}
			else
			{
				loginSession=null;
			}
			setStatus(SessionStatus.LOGOUT);
		}
		catch(final SocketException e)
		{
			synchronized(sock)
			{
				if(!Log.isMaskedErrMsg(e.getMessage())&&((!killFlag)||((sock!=null)&&sock.isConnected())))
					errorOut(e);
			}
			setStatus(SessionStatus.LOGOUT);
			preLogout(mob);
			setStatus(SessionStatus.LOGOUT1);
		}
		catch(final Exception t)
		{
			synchronized(sock)
			{
				if(!Log.isMaskedErrMsg(t.getMessage())
				&&((!killFlag)
					||(sock!=null&&sock.isConnected())))
					errorOut(t);
			}
			setStatus(SessionStatus.LOGOUT);
			preLogout(mob);
			setStatus(SessionStatus.LOGOUT1);
		}
	}

	public void logoutFinal()
	{
		try
		{
			final MOB M=mob();
			final PlayerAccount acct=this.acct;
			final String finalMsg;
			if(M!=null)
				finalMsg=M.Name()+": ";
			else
			if(acct!=null)
				finalMsg=acct.getAccountName()+": ";
			else
				finalMsg="";
			synchronized(history)
			{
				history.clear(); // will let system know you are back in login menu
			}
			if(acct!=null)
			{
				try
				{
					MOB M2=(M!=null)?M:null;
					if(M2==null)
					{
						for(final Enumeration<String> p=acct.getPlayers();p.hasMoreElements();)
						{
							final MOB M3=CMLib.players().getPlayerAllHosts(p.nextElement());
							if(M3!=null)
								M2=M3;
						}
					}
					boolean stillOnline = false;
					for(final Session S : CMLib.sessions().allIterableAllHosts())
					{
						if((S.mob()!=null)
						&&(S!=this)
						&&(S.mob().playerStats()!=null)
						&&(S.mob().playerStats().getAccount()==acct))
							stillOnline=true;
					}
					if((!stillOnline)
					&&((M2==null)||(!CMLib.flags().isCloaked(M2))))
					{
						final List<String> channels2=CMLib.channels().getFlaggedChannelNames(ChannelsLibrary.ChannelFlag.ACCOUNTLOGOFFS, M2);
						for(int i=0;i<channels2.size();i++)
							CMLib.commands().postChannel(channels2.get(i),null,L("Account @x1 has logged off.",acct.getAccountName()),true);
					}
				}
				catch(final Exception e)
				{
					Log.errOut(e);
				}
			}
			boolean setKillFlag = true;
			if(M!=null)
			{
				try
				{
					if((CMSecurity.isDisabled(CMSecurity.DisFlag.LOGOUTS)
						||(!CMLib.masking().maskCheck(CMProps.getVar(CMProps.Str.LOGOUTMASK), M, true)))
					&&(!CMSecurity.isASysOp(M)))
					{
						M.setSession(null);
						if(M.location()==null)
							M.setLocation(M.getStartRoom());
						if(M.location()==null)
							M.setLocation(CMLib.map().getStartRoom(M));
						if(M.location()==null)
							M.setLocation(CMLib.map().getRandomRoom());
						CMLib.commands().postSleep(M);
						M.setSession(this);
						M.basePhyStats().setDisposition(mob.basePhyStats().disposition()|PhyStats.IS_SLEEPING);
						M.phyStats().setDisposition(mob.phyStats().disposition()|PhyStats.IS_SLEEPING);
						setKillFlag = false;
						if(CMProps.getIntVar(CMProps.Int.LOGOUTMASKTICKS)>0)
						{
							CMLib.threads().scheduleRunnable(new Runnable() {
								final MOB M1 = M;
								final Session S1 = M.session();
								@Override
								public void run()
								{
									if(M1.session() == S1)
									{
										M1.removeFromGame(true,true);
										M1.setSession(null);
										S1.setMob(null);
									}
								}
							}, CMProps.getTickMillis() * CMProps.getIntVar(CMProps.Int.LOGOUTMASKTICKS));
						}
					}
					else
					{
						M.removeFromGame(true,true);
						M.setSession(null);
						mob=null;
					}
				}
				catch(final Exception e)
				{
					Log.errOut(e);
				}
				finally
				{
				}
			}

			setStatus(SessionStatus.LOGOUT4);
			setKillFlag(setKillFlag);
			waiting=false;
			setPromptFlag(true);
			this.acct=null;
			snoops.clear();

			closeSocks(finalMsg);
			setStatus(SessionStatus.LOGOUT5);
		}
		finally
		{
			CMLib.sessions().remove(this);
			setStatus(SessionStatus.LOGOUTFINAL);
		}
	}

	private String L(final String str, final String... xs)
	{
		return CMLib.lang().fullSessionTranslation(str, xs);
	}

	public void mainLoop() {
		final Socket sock = this.sock;
		try {
			setInputLoopTime();

			waiting = true;

			if (suspendCommandLine)
				return;

			// Read input from the user
			String input = readlineContinue();
			if (input != null) {
				lastKeystroke = System.currentTimeMillis();
				if (input.trim().length() > 0)
					prevMsgs.add(input);
				if (this.afkMessage == null)
					setAfkFlag(false);

				final MOB mob = mob();
				if (mob != null) {
					waiting = false;
					commandManager.handleInput(input, mob.playerStats(), mob);
				}
				setPromptFlag(true);
			}
			else {
				// If no input and timeout reached, send a ping
				if ((System.currentTimeMillis() - outputHandler.getLastWriteTime()) > PINGTIMEOUT)
					outputFormatter.rawCharsOut(PINGCHARS);
			}

			// Check if the character has logged out
			if (mob == null) {
				if (loginSession != null)
					loginSession.logoutLoginSession();
				setStatus(SessionStatus.LOGIN);
				return;
			}

			// Process queued commands for the mob
			while ((mob != null) && (mob.dequeCommand()) && (!killFlag)) {
			}

			// Periodic idle checks (every 60 seconds)
			final MOB mob = mob();
			if ((mob != null) && ((System.currentTimeMillis() - lastBlahCheck) >= 60000)) {
				lastBlahCheck = System.currentTimeMillis();
				final Vector<String> V = CMParms.parse(CMProps.getVar(CMProps.Str.IDLETIMERS));
				if ((V.size() > 0)
						&& (!CMSecurity.isAllowed(mob(), mob().location(), CMSecurity.SecFlag.IDLEOK))
						&& (CMath.s_int(V.firstElement()) > 0)) {
					// Log out if idle time exceeded
					final int minsIdle = (int) (getIdleMillis() / 60000);
					if (minsIdle >= CMath.s_int(V.firstElement())) {
						outputFormatter.println(CMLib.lang().L("\n\r^ZYou are being logged out!^?"));
						stopSession(true, true, false);
					}
					else if (minsIdle >= CMath.s_int(V.lastElement())) {
						// Warn about impending logout
						final int remain = CMath.s_int(V.firstElement()) - minsIdle;
						outputFormatter.println(mob(), null, null, CMLib.lang().L("\n\r^ZIf you don't do something, you will be logged out in @x1 minute(s)!^?", "" + remain));
					}
				}

				// Set AFK flag if idle for 10 minutes
				if (!isAfk()) {
					if (getIdleMillis() >= 600000) {
						setAfkFlag(true);
						if ((mob.isPlayer()) && (CMProps.getIntVar(CMProps.Int.RP_GOAFK) != 0))
							CMLib.leveler().postRPExperience(mob, "GOAFK:", null, "", CMProps.getIntVar(CMProps.Int.RP_GOAFK), false);
					}
				}
				else if ((getIdleMillis() >= 10800000) && (!isStopped()) && CMLib.flags().isInTheGame(mob(), true)) {
					// Apply "blahs" or "narcolepsy" effect if AFK for 3 hours
					if ((!CMLib.flags().isSleeping(mob))
							&& (mob().fetchEffect("Disease_Blahs") == null)
							&& (!CMSecurity.isDisabled(CMSecurity.DisFlag.AUTODISEASE))) {
						final Ability A = CMClass.getAbility("Disease_Blahs");
						if ((A != null) && (!CMSecurity.isAbilityDisabled(A.ID())))
							A.invoke(mob, mob, true, 0);
					}
					else if ((CMLib.flags().isSleeping(mob))
							&& (mob().fetchEffect("Disease_Narcolepsy") == null)
							&& (!CMSecurity.isDisabled(CMSecurity.DisFlag.AUTODISEASE))) {
						final Ability A = CMClass.getAbility("Disease_Narcolepsy");
						if ((A != null) && (!CMSecurity.isAbilityDisabled(A.ID())))
							A.invoke(mob, mob, true, 0);
					}
				}
			}

			// Show prompt if needed
			if ((needPrompt) && (waiting)) {
				if ((mob != null) && (mob.isInCombat())) {
					if (((System.currentTimeMillis() - promptLastShown) >= CMProps.getTickMillis())
							|| (input != null)) {
						showPrompt();
						setPromptFlag(false);
					}
				}
				else {
					showPrompt();
					setPromptFlag(false);
				}
			}
		} catch (final SocketException e) {
			// Handle socket exceptions
			if (!Log.isMaskedErrMsg(e.getMessage()) && ((!killFlag) || ((sock != null) && sock.isConnected())))
				errorOut(e);
			setStatus(SessionStatus.LOGOUT);
			preLogout(mob);
			setStatus(SessionStatus.LOGOUT1);
		} catch (final Exception t) {
			// Handle other exceptions
			if ((!Log.isMaskedErrMsg(t.getMessage()))
					&& ((!killFlag)
					|| (sock != null && sock.isConnected())))
				errorOut(t);
			setStatus(SessionStatus.LOGOUT);
			preLogout(mob);
			setStatus(SessionStatus.LOGOUT1);
		}
	}

	@Override
	public long activeTimeMillis()
	{
		if(activeMillis==0)
			return 0;
		return System.currentTimeMillis()-activeMillis;
	}

	public static class LoginLogoutThread implements CMRunnable, Tickable
	{
		private final long startTime=System.currentTimeMillis();

		@Override
		public String name()
		{
			return (theMOB == null) ? "Dead LLThread" : "LLThread for " + theMOB.Name();
		}

		@Override
		public boolean tick(final Tickable ticking, final int tickID)
		{
			return false;
		}

		@Override
		public String ID()
		{
			return name();
		}

		@Override
		public CMObject newInstance()
		{
			try
			{
				return getClass().getDeclaredConstructor().newInstance();
			}
			catch (final Exception e)
			{
				return new LoginLogoutThread();
			}
		}

		@Override
		public void initializeClass()
		{
		}

		@Override
		public CMObject copyOf()
		{
			try
			{
				return (CMObject) this.clone();
			}
			catch (final Exception e)
			{
				return newInstance();
			}
		}

		@Override
		public int compareTo(final CMObject o)
		{
			return CMClass.classID(this).compareToIgnoreCase(CMClass.classID(o));
		}

		@Override
		public int getTickStatus()
		{
			return 0;
		}

		@Override
		public long getStartTime()
		{
			return startTime;
		}

		@Override
		public int getGroupID()
		{
			return '0';
		}

		private MOB				theMOB			= null;
		private int				msgCode			= -1;
		private final Set<Room>	skipRooms		= new HashSet<Room>();
		private long			activeMillis	= 0;

		private LoginLogoutThread()
		{
		}

		public LoginLogoutThread(final MOB mob, final int msgC)
		{
			theMOB=mob;
			msgCode=msgC;
		}

		public void initialize()
		{
			final Set<MOB> group=theMOB.getGroupMembers(new HashSet<MOB>());
			skipRooms.clear();
			for (final Object element : group)
			{
				final MOB M=(MOB)element;
				if((M.location()!=null)&&(!skipRooms.contains(M.location())))
					skipRooms.add(M.location());
			}
			if((!CMProps.getBoolVar(CMProps.Bool.MUDSHUTTINGDOWN))
			&&(CMProps.getBoolVar(CMProps.Bool.MUDSTARTED)))
			{
				final CMMsg msg=CMClass.getMsg(theMOB,null,msgCode,null);
				Room R=theMOB.location();
				if(R!=null)
					skipRooms.remove(R);
				try
				{
					if((R!=null)&&(theMOB.location()!=null))
						R.send(theMOB,msg);
					for(final Iterator<Room> i=skipRooms.iterator();i.hasNext();)
					{
						R=i.next();
						if(theMOB.location()!=null)
							R.sendOthers(theMOB,msg);
					}
					if(R!=null)
						skipRooms.add(R);
				}
				catch (final Exception e)
				{
				}
			}
		}

		@Override
		public void run()
		{
			activeMillis=System.currentTimeMillis();
			if((!CMProps.getBoolVar(CMProps.Bool.MUDSHUTTINGDOWN))
			&&(CMProps.getBoolVar(CMProps.Bool.MUDSTARTED)))
			{
				final CMMsg msg=CMClass.getMsg(theMOB,null,msgCode,null);
				Room R=null;
				try
				{
					for(final Enumeration<Room> e=CMLib.map().rooms();e.hasMoreElements();)
					{
						R=e.nextElement();
						if((!skipRooms.contains(R))&&(theMOB.location()!=null))
							R.sendOthers(theMOB,msg);
					}
				}
				catch(final Exception e)
				{
				}
				theMOB=null;
			}
		}

		@Override
		public long activeTimeMillis()
		{
			return (activeMillis>0)?System.currentTimeMillis()-activeMillis:0;
		}
	}


	@Override
	public void setIdleTimers()
	{
		outputFormatter.resetSpamStack();
		lastKeystroke=System.currentTimeMillis();
	}

	private static enum SESS_STAT_CODES {PREVCMD,ISAFK,AFKMESSAGE,ADDRESS,IDLETIME,
										 LASTMSG,LASTNPCFIGHT,LASTPKFIGHT,TERMTYPE,
										 TOTALMILLIS,TOTALTICKS,WRAP,LASTLOOPTIME,
										 ROOMLOOK,TWRAP,PPING,CPING}

	@Override
	public int getSaveStatIndex()
	{
		return SESS_STAT_CODES.values().length;
	}

	@Override
	public String[] getStatCodes()
	{
		return CMParms.toStringArray(SESS_STAT_CODES.values());
	}

	@Override
	public boolean isStat(final String code)
	{
		return getStatIndex(code) != null;
	}

	private SESS_STAT_CODES getStatIndex(final String code)
	{
		return (SESS_STAT_CODES) CMath.s_valueOf(SESS_STAT_CODES.values(), code);
	}

	@Override
	public String getStat(final String code)
	{
		final SESS_STAT_CODES stat = getStatIndex(code);
		if (stat == null)
		{
			return "";
		}
		switch (stat)
		{
		case PREVCMD:
			return CMParms.combineQuoted(getPreviousCMD(), 0);
		case ISAFK:
			return "" + isAfk();
		case AFKMESSAGE:
			return getAfkMessage();
		case ADDRESS:
			return getAddress();
		case IDLETIME:
			return CMLib.time().date2String(System.currentTimeMillis() - getIdleMillis());
		case LASTMSG:
			return CMParms.combineQuoted(getLastMsgs(), 0);
		case LASTNPCFIGHT:
			return CMLib.time().date2String(getLastNPCFight());
		case LASTPKFIGHT:
			return CMLib.time().date2String(getLastPKFight());
		case TERMTYPE:
			return getTerminalType();
		case TOTALMILLIS:
			// TODO Review actual usage vs getter usage
			// FIXME totalmillis is a duration measure, unclear logic.
			return CMLib.time().date2String(System.currentTimeMillis() - getTotalMillis());
		case TOTALTICKS:
			return "" + getTotalTicks();
		case WRAP:
			return "" + getWrap();
		case LASTLOOPTIME:
			return CMLib.time().date2String(getInputLoopTime());
		case ROOMLOOK:
		case PPING:
		case CPING:
			break; // do nothing
		case TWRAP:
			return ""+this.terminalWidth;
		default:
			Log.errOut("Session", "getStat:Unhandled:" + stat.toString());
			break;
		}
		return null;
	}

	@Override
	public void setStat(final String code, final String val)
	{
		final SESS_STAT_CODES stat = getStatIndex(code);
		if (stat == null)
		{
			return;
		}
		switch (stat)
		{
		case PREVCMD:
			setPreviousCmd(CMParms.parse(val));
			break;
		case ISAFK:
			setAfkFlag(CMath.s_bool(val));
			break;
		case AFKMESSAGE:
			afkMessage = val;
			break;
		case ADDRESS:
			return;
		case IDLETIME:
			lastKeystroke = CMLib.time().string2Millis(val);
			break;
		case LASTMSG:
			prevMsgs = CMParms.parse(val);
			break;
		case LASTNPCFIGHT:
			lastNPCFight = CMLib.time().string2Millis(val);
			break;
		case LASTPKFIGHT:
			lastPKFight = CMLib.time().string2Millis(val);
			break;
		case TERMTYPE:
			terminalType = val;
			break;
		case TOTALMILLIS:
			// FIXME totalmillis is a duration measure, unclear logic.
			// milliTotal = System.currentTimeMillis() - CMLib.time().string2Millis(val);
			Log.warnOut("DefaultSession", "setStat: Attempt to set deprecated TOTALMILLIS stat. This operation is no longer supported.");
			break;
		case TOTALTICKS:
			tickTotal = CMath.s_int(val);
			break;
		case TWRAP:
			this.terminalWidth = CMath.s_int(val);
			break;
		case WRAP:
			if ((mob != null) && (mob.playerStats() != null))
				mob.playerStats().setWrap(CMath.s_int(val));
			break;
		case LASTLOOPTIME:
			lastLoopTop = CMLib.time().string2Millis(val);
			break;
		case CPING:
			if(getClientTelnetMode(TELNET_GMCP))
			{
				this.gmcpPings.clear();
				this.doProtocolPings();
			}
			break;
		case PPING:
			this.gmcpPings.remove("system.nextMedReport");
			this.doProtocolPings();
			break;
		case ROOMLOOK:
			if (val == null || val.isEmpty() || !RoomState.canDecode(val)) {
				Log.errOut("ROOMLOOK triggered with an unset, empty or invalid value.");
				break;
			}

			RoomState roomState = RoomState.decode(val);
			lastSeenRoomID = roomState.getRoomId();
			lastRoomHash = roomState.getRoomHash();

			if(getClientTelnetMode(TELNET_GMCP))
			{
				final byte[] gmcpPingBuf=CMLib.protocol().invokeRoomChangeGmcp(this, gmcpPings, gmcpSupports, msdpReportables, lastSeenRoomID, lastRoomHash);
				if(gmcpPingBuf!=null)
				{
					try
					{
						rawBytesOut(gmcpPingBuf);
					}
					catch (final IOException e)
					{
						Log.errOut(e);
					}
				}
			}
			break;
		default:
			Log.errOut("Session", "setStat:Unhandled:" + stat.toString());
			break;
		}
	}

	@Override
	public String getLastSeenRoomID() {
		return lastSeenRoomID;
	}

	@Override
	public Integer getLastSeenRoomHash() {
		return lastRoomHash;
	}

	@Override
	public OutputFormatter getOutputFormatter() {
		return outputFormatter;
	}
}
