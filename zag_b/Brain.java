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
	int state = 1;
	int flag_state = 0;

	// others
	Point initial_position = new Point(-30, 14);
	Point absolute_position = new Point(0, 0);
	String flag1, flag2;
	boolean bola_perto = false;
	double angle = 0;
	String opposite_team;

	// constants
	public static final double KICK_FORCE = 3;
	public static final double TURN_ANGLE = 44;
	public static final double LOW_SPEED = 5;
	public static final double BALL_DISTANCE = 0.7;

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
					// agente esta direcionado para a bola
					if (!teammateIsCloser(teammate)) {
						// va ate ela pois nao tem amigo mais perto
						if (ballIsClose()) {
							// pass(teammate);
							System.out.println("toca");
						} else {
							run(ball);
						}
						// state = 1;
					} else {
						// bola esta distante
						// segue oponente
						// ou anda devagar em direcao a bola
						moveWithoutLeavingArea(ball);
					}
				}

			break;
			case 1:
/*		
				if (ball == null) {
					// If you don't know where the ball is, then find it
					m_agent.turn(TURN_ANGLE);
					angle = angle + TURN_ANGLE;

				} else if (ballIsClose()) {
					// System.out.println("bola perto: " + ball.m_distance);
				
					if (ball.m_distance > 5.0 && (teammate != null && teammate.m_distance < ball.m_distance)) {
						m_agent.turn(ball.m_direction);
						// angle = angle + TURN_ANGLE;
						
					} else if (ball.m_distance > 0.7) {
						// If ball is too far then
						// turn to ball or 
						// if we have correct direction then go to ball
						if (!objectIsAligned(ball)) {
							m_agent.turn(ball.m_direction);
							angle = angle + ball.m_direction;
						}
						else { // otherwise run toward the ball
							run(ball);
						}
						
					} else {
						if (teammate == null) {
							m_agent.turn(TURN_ANGLE);
							m_memory.clearInfo(); //it was waitForNewInfo(), but I don't think it needs to explicitly wait --Edgar
						} else {
							m_agent.kick(KICK_FORCE*teammate.m_distance, teammate.m_direction);
							state = 0;
						}
					}
				} else if (bola_perto == true) {
					state = 0;
					bola_perto = false;
				}
*/				state = 0;
			break;
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
		dash_factor *= 0.9; //this decreases the dash factor, it is restored when we get new visual info
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
		if (ball != null && ball.m_distance < BALL_DISTANCE)
			return true;

		return false;
	}

	public boolean objectIsAligned(ObjectInfo object) {
		if (object.m_direction < -2.5 || object.m_direction > 2.5)
			return false;

		return true;
	}

	public void moveWithoutLeavingArea(ObjectInfo object) {
		m_agent.dash(object.m_distance * LOW_SPEED);
	}

	public boolean teammateIsCloser(ObjectInfo teammate) {
		if (teammate != null && teammate.m_distance < ball.m_distance)
			return true;

		return false;
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

		if (m_side == 'l')
			flag1 = new String ("flag p l b");
		else	
			flag1 = new String ("flag p r b");
		
		flag2 = new String ("flag c t");
    }

    // Private members
    private SendCommand m_agent;			// robot which is controled by this brain
    private Memory m_memory;				// place where all information is stored
    private char m_side;
    volatile private boolean m_timeOver,m_waiting;
    private String m_team;
    volatile private String m_playMode;
    volatile private float dash_factor=20; //
}
