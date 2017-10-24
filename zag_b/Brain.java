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

	// states
	int estado = 1;
	int flag_state = 0;

	// others
	Point position = new Point(-30, 14);
	String flag1, flag2;
	int limit = 15;
	boolean bola_perto = false;
	double angle = 0;
	double kick_force = 3;

    public Brain(SendCommand agent, 
		 String team, 
		 char side, 
		 int number, 
		 String playMode) {
		m_timeOver = false;
		m_agent = agent;
		m_memory = new Memory();
		m_team = team;
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

			switch (estado) {
			case 0:
				if (ballIsClose()) {
					estado = 1;
					bola_perto = true;
				}

				locate();

			break;
			case 1:
				object = m_memory.getObject("player " + m_team);

				if (ball == null) {
					// If you don't know where the ball is, then find it
					m_agent.turn(44);
					angle = angle + 44;

				} else if (ballIsClose()) {
					// System.out.println("bola perto: " + ball.m_distance);
				
					if (ball.m_distance > 5.0 && (object != null && object.m_distance < ball.m_distance)) {
						m_agent.turn(ball.m_direction);
						// angle = angle + 44;
						
					} else if (ball.m_distance > 0.7) {
						// If ball is too far then
						// turn to ball or 
						// if we have correct direction then go to ball
						if (ball.m_direction < -2.5 || ball.m_direction > 2.5) {
							m_agent.turn(ball.m_direction);
							angle = angle + ball.m_direction;
						}
						else { // otherwise run toward the ball
							calculatedDash(ball);
						}
						
					} else {
						if (object == null) {
							m_agent.turn(44);
							m_memory.clearInfo(); //it was waitForNewInfo(), but I don't think it needs to explicitly wait --Edgar
						} else {
							m_agent.kick(kick_force*object.m_distance, object.m_direction);
							estado = 0;
						}
					}
				} else if (bola_perto == true) {
					estado = 0;
					bola_perto = false;
				}
			break;
			}
			// sleep one step to ensure that we will not send
			// two commands in one cycle.
			try {
				Thread.sleep(2*SoccerParams.simulator_step);
			} catch(Exception e) {}
			m_waiting=true;
			}
		m_agent.bye();
    }

    // Here are suporting functions for implement logic
	public void calculatedDash(ObjectInfo parameter) {
		float power=dash_factor*parameter.m_distance;
		dash_factor*=0.9; //this decreases the dash factor, it is restored when we get new visual info
		if (power > 100)
			power=100;
		m_agent.dash(power);
	}

	public boolean ballIsClose() {
		if (ball != null && ball.m_distance < limit)
			return true;

		return false;
	}
	
	public void locate() {
		if (flag_state == 0) {
			// primeira flag
			flag = m_memory.getObject(flag1);

			if (flag == null) {
				m_agent.turn(44);
				angle = angle + 44;
			
			} else if (flag.m_distance > 2) {
				if (flag.m_direction < -2.5 || flag.m_direction > 2.5) {
					m_agent.turn(flag.m_direction);
				} else {
					calculatedDash(flag);
				}
			} else {
				flag_state = 1;
			}

		} else {
			// System.out.println("flag_state = 1");
			// segunda flag
			flag = m_memory.getObject(flag2);
			
			if (flag == null) {
				m_agent.turn(-44);
				angle = angle - 44;
			
			} else if (flag.m_distance > 55) {
				if (flag.m_direction < -2.5 || flag.m_direction > 2.5) {
					m_agent.turn(flag.m_direction);
					angle = angle + flag.m_direction;
				} else {
					calculatedDash(flag);
				}
			}else{
				flag_state = 0;
				estado = 1;
				angle = 0;
			}
		}
	}

    public void see(VisualInfo info) {
		m_memory.store(info);
		dash_factor=20;
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
	    m_agent.move(position.x, position.y);
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
