package dotslines;
import java.awt.*;
import javax.swing.*;

import java.awt.geom.Line2D;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.Observable;
import java.util.Observer;

import java.util.*;

import java.lang.*;

public class Main extends JFrame {
	
	public Main() {
		
	}
	
	public static void main(String[] args) {
		
		Canvas canvas = new Canvas();
		Reporter reporter = new Reporter();
		Repository repo = Repository.getInstance();
		
		JFrame frame = new JFrame();
		frame.setSize(400, 400);
		frame.setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.add(canvas);
		frame.addMouseListener(reporter);
		frame.setVisible(true);
		
		repo.getInstance().addObserver(canvas);
		
		

	}

}

class Canvas extends JPanel implements Observer {
	
	public void paintComponent (Graphics g) {
		super.paintComponent(g);;
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, 400, 400);
		g.fillRect(0, 0, 400, 400);
		
		
		// draw Points
		g.setColor(Color.CYAN);
		Stack<Point> points = Repository.getInstance().getPoints();
		
		for (int i = 0; i < points.size(); i++) {
			int x = points.get(i).getX();
			int y = points.get(i).getY();
			g.fillOval(x-13, y-33, 10, 10);
		}
		
		// draw Lines
		g.setColor(Color.PINK);
		Stack<Line> lines = Repository.getInstance().getLines();
		
		for (int i = 0; i < lines.size(); i++) {
			int x1 = lines.get(i).getPointOne().getX();
			int y1 = lines.get(i).getPointOne().getY();
			int x2 = lines.get(i).getPointTwo().getX();
			int y2 = lines.get(i).getPointTwo().getY();
			g.drawLine(x1-7, y1-27, x2-7, y2-27);
		}
		
	}
	
	@Override
	public void update(Observable o, Object arg) {
		repaint();
	}
	
}

class Reporter implements MouseListener{
	
	@Override
	public void mouseClicked(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		
		Repository.getInstance().addPoint(x, y);
		
		System.out.println("x = " + x + ", y = " + y);
	}
	
	public void mousePressed(MouseEvent e) {
		
	}
	
	public void mouseReleased(MouseEvent e) {
		
	}
	
	public void mouseEntered(MouseEvent e) {
		
	}
	
	public void mouseExited(MouseEvent e) {
		
	}
}

class Repository extends Observable {
	
	private double totalDistance;
	private static Repository instance;
	
	private Stack<Point> points = new Stack<Point>();
	private Stack<Line> lines = new Stack<Line>();
	
	public boolean isFree = true;
	
	
	private Repository() {
		LineMaker lineCalc = new LineMaker();
		PointDestroyer pointRemove = new PointDestroyer();
		
		
		Thread thread_1 = new Thread(lineCalc);
		Thread thread_2 = new Thread(pointRemove);
		thread_1.start();
		thread_2.start();
	}
	
	public static Repository getInstance() {
		
		if (instance == null) {
			instance = new Repository();
		}
		
		return instance;
	}
	
	public Stack<Point> getPoints() {
		
		return points;
		
	}
	
	public synchronized void addPoint(int x, int y) {
		
		while(!isFree) {
			try {
				System.out.println("isFree is false aaaaahhhh");
			    wait();
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}
		}
		
		isFree = false;
		
		Point newpoint = new Point();
		
		newpoint.setX(x);
		newpoint.setY(y);
		
		points.push(newpoint);
		
		System.out.println("Point added!");
		isFree = true;
		
		setChanged();
		notifyObservers();
		notify();
	}
	
	public synchronized void addLine(Point pointOne, Point pointTwo) {
		
		while(!isFree) {
			try {
			    wait();
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}
		}
		
		isFree = false;
		
		Line newline = new Line();
		
		newline.setPointOne(pointOne);
		newline.setPointTwo(pointTwo);
		
		if(!lines.isEmpty()) {
			//check to make sure we didn't just add this line
			if(!((lines.peek().getPointOne() == pointOne && lines.peek().getPointTwo() == pointTwo) || (lines.peek().getPointTwo() == pointOne && lines.peek().getPointOne() == pointTwo))) {
				
				pointOne.iterateLineCount();
				pointTwo.iterateLineCount();
				
				lines.push(newline);
				
				System.out.println("Line added!");
				
			}
		}
		else //if the stack is empty we don't need to do that other check
		{
			pointOne.iterateLineCount();
			pointTwo.iterateLineCount();
			
			lines.push(newline);
			
			System.out.println("Line added!");
		}
		
		
		setChanged();
		notifyObservers();
		isFree = true;
		notify();
		
	}
	
	public synchronized void removePoint(int i) {
		
		while(!isFree) {
			try {
			    wait();
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}
		}
		
		isFree = false;
		
		points.get(i).setRemoved();
		points.remove(i);
		System.out.println("Point removed!");
		
		setChanged();
		notifyObservers();
		isFree = true;
		notify();
		
	}
	
	public synchronized void removeLines() {
		
		while(!isFree) {
			try {
			    wait();
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}
		}
		
		isFree = false;
		
		/**OLD VERSION
		lines.remove(i);
		System.out.println("Line " + i + " removed!");
		**/
		
		for(Iterator<Line> iterator = lines.iterator(); iterator.hasNext();) {
			Line currLine = iterator.next();
			
			if (currLine.removeCheck()) {
				iterator.remove();
				
				//allow other point to have line recalculated
				if ((!currLine.getPointOne().checkRemoved()) && (currLine.getPointOne().connectCheck())) {
					currLine.getPointOne().connectSwitch();
				}
				else if ((!currLine.getPointTwo().checkRemoved()) && (currLine.getPointTwo().connectCheck())) {
					currLine.getPointTwo().connectSwitch();
				}
				
			}
			
		}
		
		setChanged();
		notifyObservers();
		isFree = true;
		notify();
		
	}
	
	public Stack<Line> getLines() {
		
		return lines;
		
	}
}

class LineMaker implements Runnable{ //KnowledgeSource
	
	@Override
	public void run() {
		Stack<Point>points = Repository.getInstance().getPoints();
		while (true) {
			//System.out.println("LineMaker is running!");
			for (int i = 0; i < points.size(); i++) { //iterate through all points
				Point pointOne = points.get(i);
				Point pointTwo = new Point();
				double minDistance = 1000.0;
				double currDistance;
				
				if (!pointOne.connectCheck() && points.size() > 1) { //if current point isn't already connected
					
					for (int j = 0; j < points.size(); j++) { //iterate through all points
						Point tempPoint = points.get(j);
						
						if(i != j) { // if second point isn't the same point
							int x1, y1, x2, y2;
							
							x1 = pointOne.getX();
							y1 = pointOne.getY();
							x2 = tempPoint.getX();
							y2 = tempPoint.getY();
							
							//calculate distance between two points
							currDistance = Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
							
							if ((currDistance < minDistance) && !(tempPoint.checkRemoved())) {
								minDistance = currDistance;
								pointTwo = tempPoint;
							}
							
						}
						
					}
					

					Repository.getInstance().addLine(pointOne, pointTwo);
					pointOne.connectSwitch();
					
					if(!pointTwo.connectCheck()) {
						pointTwo.connectSwitch();
					}
					
				}
				
			}
			/**
			try {
				   Thread.sleep(100);
				} catch (InterruptedException ex) {}
			**/
		}
		
	}
}

class PointDestroyer implements Runnable{ //Controller
	
	@Override
	public void run() {
		Stack<Point> points = Repository.getInstance().getPoints();
		Stack<Line> lines = Repository.getInstance().getLines();
		while(true) {
			
			
			for (int i = 0; i < points.size(); i++) { //iterate through all points
				
				Point currPoint = points.get(i);
				
				if(currPoint.getLineCount() == 5) { //point has 5 lines, time to delete it
					
					Iterator<Line> iter = lines.iterator();
					
					while (iter.hasNext()) { //iterate through all lines
						
						Line currLine = iter.next();
						
						if(currLine.getPointOne() == currPoint || currLine.getPointTwo() == currPoint) { //if current line has current point as one of the points
							
							currLine.removeSet();
							
							currLine.getPointOne().decrementLineCount();
							currLine.getPointTwo().decrementLineCount();
						}
						
						
					}
					
					Repository.getInstance().removePoint(i);
					Repository.getInstance().removeLines();
					
					
					//calculate new lines for the rest of the points
					
					/**[SOMETHING LIKE THIS] allow other points to have their line recalculated 
					if ((currLine.getPointOne().getLineCount() == 0) && (currLine.getPointOne() != currPoint)) {
						currLine.getPointOne().connectSwitch();
					}
					else if ((currLine.getPointTwo().getLineCount() == 0) && (currLine.getPointTwo() != currPoint)) {
						currLine.getPointTwo().connectSwitch();
					}
					**/
					
				}
			}
			
			/**
			try {
				   Thread.sleep(100);
				} catch (InterruptedException ex) {}
			**/
		}
		
	}
}

class Point {
	private int x;
	private int y;
	private boolean isConnected = false;
	private boolean removed = false;
	
	private int lineCount;
	
	public Point() {
		lineCount = 0;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public synchronized boolean connectCheck() {
		return isConnected;
	}
	
	public synchronized void connectSwitch() {
		if (isConnected) {
			isConnected = false;
		}
		else
		{
			isConnected = true;
		}
	}
	
	public synchronized int getLineCount() {
		return lineCount;
	}
	
	public synchronized void iterateLineCount() {
		lineCount++;
	}
	
	public synchronized void decrementLineCount() {
		lineCount--;
	}
	
	public synchronized void zeroLineCount() {
		lineCount = 0;
	}
	
	public synchronized void setRemoved() {
		removed = true;
	}
	
	public synchronized boolean checkRemoved() {
		return removed;
	}
}

class Line {
	
	private Point pointOne = new Point();
	private Point pointTwo = new Point();
	
	private boolean removeMark = false;
	
	public void setPointOne(Point point) {
		pointOne = point;
	}
	
	public void setPointTwo(Point point) {
		pointTwo = point;
	}
	
	public Point getPointOne() {
		return pointOne;
	}
	
	public Point getPointTwo() {
		return pointTwo;
	}
	
	public boolean removeCheck() {
		return removeMark;
	}
	
	public void removeSet() {
		removeMark = true;
	}
	
}