//
//	File:			Brain.java
//	Author:		Krzysztof Langner
//	Date:			1997/04/28
//
//    Modified by:	Paul Marlow

//    Modified by:      Edgar Acosta
//    Date:             March 4, 2008

// Zagueiro inferior (zag_b)

import java.lang.Math;
import java.util.regex.*;

class Brain extends Thread implements SensorInput {
	// GLOBAL VARIABLES
	// objects
	ObjectInfo object;
	ObjectInfo ball;
	ObjectInfo flag;
	ObjectInfo teammate;

	// states
	int state = 0;
	int scan_state = 0;
	int flag_state = 0;

	// others
	Point initial_position = new Point(-30, 14);
	Point absolute_position = new Point(0, 0);
	boolean bola_perto = false;
	double angle = 0;
	String opposite_team;

	// position flags
	String flag1;
	String flag2;



	// constants
	public static final double KICK_FORCE = 3;
	public static final double TURN_ANGLE = 44;
	public static final double LOW_SPEED = 5;
	public static final double BALL_DIST_KICK = 0.7;
	public static final double BALL_SECURE_DIST = 20;

    public Brain(SendCommand agent, 
		 String team, 
		 char side, 
		 int number, 
		 String playMode) {
		m_timeOver = false;
		m_agent = agent;
		m_memory = new Memory();
		
		m_team = team;
		if (m_team == "yellow")
			opposite_team = new String("blue");
		
		m_side = side;
		if (m_side == 'l')
			flag1 = new String ("flag p l t");
		else
			flag1 = new String ("flag p r t");
		
		flag2 = new String ("flag c b");
		
		// m_number = number;
		m_playMode = playMode;
		m_waiting=true;

		start();
    }

	class Point {
		public double x;
		public double y;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}

    public void run() {
		// o scanned, uma vez setado para true, nunca volta para false (ajeitar)
		while(!m_timeOver) {
			while(m_waiting || m_memory.isEmpty()) {}
			// before a kick off move somewhere on my side
			if (Pattern.matches("^before_kick_off.*",m_playMode))
				moveToMySide();

			ball = m_memory.getObject("ball");
			teammate = m_memory.getObject("player " + m_team);

			switch (state) {
			case 0:
				if (!needToLook(ball)) {
					if (teammate != null && teammate.m_distance < ball.m_distance) {
						// tem alguem mais perto da bola, anda devagar ate ela
						moveWithoutLeavingArea(ball);

					} else {
						System.out.println("estou mais perto");
						// va ate ela pois nao tem amigo mais perto
						if (ballIsClose()) {
							// muda de estado para o estado que toca
							state = 1;
						} else {
							run(ball);
						}
					}
				}
			break;
			case 1: // tocar a bola
				System.out.println("toca");
				if (!needToLook(teammate)) {
					// tocou
					// agente esta direcionado para o parceiro
					pass(teammate);
					state = 2; // reposicionar
				}
			break;
			case 2: // reposicionar
				System.out.println("reposicionar");
				if (flag_state == 0) {
					// System.out.println("flag_state = 0");
					// primeira flag
					flag = m_memory.getObject(flag1);

					if (flag == null) {
						m_agent.turn(44);
					
					} else if (flag.m_distance > 2) {
						if (flag.m_direction < -2.5 || flag.m_direction > 2.5) {
							m_agent.turn(flag.m_direction);
						} else {
							run(flag);
						}
					} else {
						flag_state = 1;
					}

				} else {
					// System.out.println("flag_state = 1");
					// segunda flag
					flag = m_memory.getObject(flag2);
					
					if (flag == null) {
						m_agent.turn(TURN_ANGLE);
					
					} else if (flag.m_distance > 55) {
						if (flag.m_direction < -2.5 || flag.m_direction > 2.5) {
							m_agent.turn(flag.m_direction);
							
						} else {
							run(flag);
						}
					} else {
						flag_state = 0;
						state = 0;
					}
				}
			}

			// sleep one step to ensure that we will not send
			// two commands in one cycle.
			try {
				Thread.sleep(2*SoccerParams.simulator_step);
			} catch(Exception e) {}
			m_waiting = true;
		}
		m_agent.bye();
    }


	public void run(ObjectInfo object) {
		float power = dash_factor * object.m_distance;
		// decreases the dash factor, it's restored when we get new visual info
		dash_factor *= 0.9;
		if (power > 100)
			power = 100;
		m_agent.dash(power);
	}

	public boolean needToLook(ObjectInfo object) {
		// toda vez que o agente faz algum movimento a procura da bola,
		// retorna true (precisou procurar objeto)
		if (object == null) {
			m_agent.turn(TURN_ANGLE);
			return true;

		} else if (!objectIsAligned(object)) {
			m_agent.turn(object.m_direction);
			return true;
		}

		return false;
	}

	public boolean ballIsClose() {
		if (ball != null && ball.m_distance < BALL_DIST_KICK)
			return true;

		return false;
	}

	public boolean objectIsAligned(ObjectInfo object) {
		if (object.m_direction < -2.5 || object.m_direction > 2.5)
			return false;

		return true;
	}

	public void moveWithoutLeavingArea(ObjectInfo object) {
		System.out.println("movingWithoutLeavingArea()");
		m_agent.dash(object.m_distance * LOW_SPEED);
	}

	public boolean teammateIsCloser(ObjectInfo teammate) {
		System.out.println("amigo: " + teammate.m_distance);
		System.out.println("bola: " + ball.m_distance);
		if (teammate != null && teammate.m_distance < ball.m_distance) {
			if (teammate == null) {
				System.out.println("cade ele?");
			}
			return true;
		}

		return false;
	}

	public void pass(ObjectInfo temmate) {
		m_agent.kick(KICK_FORCE*teammate.m_distance, teammate.m_direction);
	}

    public void see(VisualInfo info) {
		m_memory.store(info);
		dash_factor = 20;
    }

    // This function receives hear information from player
    public void hear(int time, int direction, String message) {
    }

    // This function receives hear information from referee
    public void hear(int time, String message) {						 
		if (message.compareTo("time_over") == 0)
			m_timeOver = true;
		//the next should take into account all play modes, by now is good
		if (Pattern.matches("^before_kick_off.*",message))
			m_playMode=message;
    }

    // This function receives a sense_body message
    // It doesn't do anything other than signaling the time to send a command
    public void sense(String message) {
		m_waiting=false;
    }

    public void moveToMySide() {
	    m_agent.move(initial_position.x, initial_position.y);
	    m_playMode="!"+m_playMode;
    }

    // Private members
    private SendCommand m_agent; // robot which is controled by this brain
    private Memory m_memory; // place where all information is stored
    private char m_side;
    volatile private boolean m_timeOver,m_waiting;
    private String m_team;
    volatile private String m_playMode;
    volatile private float dash_factor=20;
}
