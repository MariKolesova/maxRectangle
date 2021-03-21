/*
 * Swing version.
 */

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;  //Vector
import java.lang.*; //Math

public class RectangleFind extends JApplet{
    JLabel label;
    JPanel buttonPane;
    JButton b2;

    ConvexHull polygon;
    RectangleArea rectangleArea;
    MyListener myListener;

    public void init() {
        buildUI(getContentPane());
    }

    void buildUI(Container container) {
        container.setLayout(new BoxLayout(container,
            BoxLayout.Y_AXIS));

        label = new JLabel();

        polygon = new ConvexHull();

        rectangleArea = new RectangleArea(this);

        myListener = new MyListener(this);
        rectangleArea.addMouseListener(myListener);

        container.add(rectangleArea);

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        b2 = new JButton("Максимальный прямоугольник");
        b2.addActionListener(e -> {
            polygon.computeLargestRectangle();
            rectangleArea.displayedRect = 6;
            polygon.status = 16;
            rectangleArea.repaint();
        });

        buttonPane.add(Box.createHorizontalStrut(10)); //горизонтальная склейка
        buttonPane.add(b2);

        container.add(buttonPane);

        //Выровнивание левых краев компонентов
        rectangleArea.setAlignmentX(LEFT_ALIGNMENT);
        label.setAlignmentX(LEFT_ALIGNMENT); //unnecessary, but doesn't hurt
        buttonPane.setAlignmentX(LEFT_ALIGNMENT);
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        RectangleFind controller = new RectangleFind();
        controller.buildUI(f.getContentPane());
        f.pack();
        f.setVisible(true);
    }
}

class MyListener extends MouseInputAdapter{
    RectangleFind rf;

    public MyListener(RectangleFind rf){
        this.rf = rf;
    }
    public void mousePressed(MouseEvent e) {

        int x = e.getX();
        int y = e.getY();

        GeomPoint p = new GeomPoint(x,y);

        if (this.rf.polygon.size() <2){
            this.rf.polygon.add(p);
            this.rf.polygon.status = 1;
            this.rf.polygon.changed = true;
        }

        else if (this.rf.polygon.size() == 2){
            GeomPoint ha = (GeomPoint)this.rf.polygon.elementAt(0);
            GeomPoint hb = (GeomPoint)this.rf.polygon.elementAt(1);
            if (this.rf.polygon.onLeft(ha, hb, p)){
                this.rf.polygon.add(p);
                this.rf.polygon.status = 2;
                this.rf.polygon.changed = true;
            }
            else{
                this.rf.polygon.insertElementAt(p, 1);
                this.rf.polygon.status = 2;
            }
        }
        else{
            if (this.rf.polygon.addPointToHull(p)){
                this.rf.polygon.status = 2;
            }
            else{
                this.rf.polygon.status = 3;
            }
        }

        this.rf.rectangleArea.repaint();

    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}


class RectangleArea extends JPanel {

    RectangleFind controller;
    Dimension preferredSize = new Dimension(600,450);
    public int displayedRect = 6;

    public RectangleArea(RectangleFind controller) {
        this.controller = controller;

        Border raisedBevel = BorderFactory.createRaisedBevelBorder();
        Border loweredBevel = BorderFactory.createLoweredBevelBorder();
        Border compound = BorderFactory.createCompoundBorder
            (raisedBevel, loweredBevel);
        setBorder(compound);

    }

    public Dimension getPreferredSize() {
        return preferredSize;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);  //закраска фона

        final ConvexHull hull = this.controller.polygon;

        GeomPoint point = null;
        GeomPoint prevPoint = null;

        for(int i=0; i<hull.size(); i++){

            if(i==0){
                prevPoint = (GeomPoint)hull.elementAt(hull.size()-1);
            }

            point = (GeomPoint)hull.elementAt(i);

            g.setColor(Color.black);
            g.fillOval(point.x-2, point.y-2, 5, 5);

            if(prevPoint != null){
                g.fillOval(prevPoint.x-2, prevPoint.y-2,5,5);
                g.drawLine(point.x, point.y, prevPoint.x, prevPoint.y);
            }
            prevPoint = point;
        }
        if(hull.rectp !=null){
            if (displayedRect ==6){
                g.setColor(Color.red);
            }
            else{
                g.setColor(Color.magenta);
            }
            Rectangle lr = (Rectangle)hull.RectList.elementAt(displayedRect);

            g.drawRect(lr.x,lr.y, lr.width, lr.height);
            g.fillRect(lr.x,lr.y, lr.width, lr.height);
        }

    }
}

class GeomPoint extends Point {

    public GeomPoint(int ptx, int pty){
        this.x = ptx;
        this.y = pty;
    }

    int min(int a, int b){
        if(a<=b) return a; else return b;
    }

    int max(int a, int b){
        if (a>=b) return a; else return b;
    }
}


class GeomEdge{

    int xmin, xmax; /* горизонтально, +x is справа */
    int ymin, ymax; /* вертикально, +y снизу */
    double m,b; /* y = mx + b */
    boolean isTop, isRight; /* положение края относительно корпус */

    public GeomEdge(GeomPoint p, GeomPoint q){
        this.xmin = p.min(p.x, q.x);
        this.xmax = p.max(p.x, q.x);
        this.ymin = p.min(p.y, q.y);
        this.ymax = p.max(p.y, q.y);
        this.m = ((double)(q.y-p.y))/((double)(q.x-p.x));
        this.b = p.y - m*(p.x);
        this.isTop = p.x > q.x; //край справа налево (против часовой стрелки)
        this.isRight = p.y > q.y; //край снизу вверх
    }
}


class ConvexHull extends Vector {

    int status;
    private int start, stop; //касательные для итеративной выпуклой оболочки
    private int xmin,xmax,ymin,ymax;  //position
    int yxmax; //y coord of xmax
    GeomPoint rectp;
    int recth, rectw;
    boolean changed;

    /* самые большие прямоугольники с углами на AC, BD, ABC, ABD, ACD, BCD */
    Vector RectList;

    /* фиксированное соотношение сторон */
    private boolean fixed;
    private int fixedX, fixedY;

    public ConvexHull() {
        this.fixed = false;
        this.fixedX = 1;
        this.fixedY = 1;
        RectList = new Vector();
    }

    /* положение точки относительно края корпуса
     * знак удвоенной площади треугольника abc
     */
    boolean onLeft(GeomPoint a, GeomPoint b, GeomPoint c){
        int area = (b.x -a.x)*(c.y - a.y) - (c.x - a.x)*(b.y - a.y);
        return (area<0);
    }

    /* проверка, находится ли точка снаружи
     * истина - точка находится справа от всех вершин
     * находит касательные, если точка находится за пределами
     */
    boolean pointOutside(GeomPoint p){//, int start, int stop){

        boolean ptIn = true, currIn, prevIn = true;

        GeomPoint a = (GeomPoint)this.elementAt(0);
        GeomPoint b;

        for(int i=0; i<this.size(); i++){

            b = (GeomPoint)this.elementAt((i+1)%this.size());
            currIn = onLeft(a, b, p);
            ptIn = ptIn && currIn;
            a = b;

            if(prevIn && !currIn){ start = i;} /* следующая точка снаружи, 1-я касательная основа */
            if(!prevIn && currIn){ stop = i;}  /* 2-я касательная */
            prevIn = currIn;

        }
        return !ptIn;
    }

    /* проверка, находится ли точка снаружи, вставить ее, сохраняя общее положение */
    boolean addPointToHull(GeomPoint p) {

        /* индекс касательных */
        start=0;
        stop=0;

        if(!pointOutside(p)){
            return false;
        }

        /* вставить точку */
        int numRemove;

        if (stop > start){
            numRemove = stop-start-1;
            if (numRemove>0){
                this.removeRange(start+1, stop);
            }
            this.insertElementAt(p, start+1); //insertElmentAt(p, start+1);
        }
        else{
            numRemove = stop+this.size()-start-1;
            if (numRemove > 0){
                if (start+1 < this.size()){
                    this.removeRange(start+1, this.size());
                }
                if(stop-1 >= 0){
                    this.removeRange(0, stop);
                }
            }
            this.add(p);

        }
        this.changed = true;
        return true;
    }

    /* вычислить список ребер
     * установить xmin, xmax
     * используется для поиска самого большого прямоугольника путем сканирования по горизонтали
     */
    Vector computeEdgeList(){
        Vector l = new Vector();
        GeomPoint a,b;
        GeomEdge e;
        a = (GeomPoint)this.elementAt(this.size()-1);
        for(int i=0; i<this.size(); i++){
            b = (GeomPoint)this.elementAt(i);

            if (i==0){
                this.xmin = a.x;
                this.xmax = a.x;
                this.ymin = a.y;
                this.ymax = a.y;
            }
            else{
                if (a.x < this.xmin){
                    this.xmin = a.x;
                }
                if (a.x > this.xmax){
                    this.xmax  = a.x;
                    this.yxmax = a.y;
                }
                if (a.y < this.ymin){
                    this.ymin = a.y;
                }
                if (a.y > this.ymax){
                    this.ymax  = a.y;
                }
            }
            e = new GeomEdge(a,b);
            l.add(e);
            a = b;
        }
        return l;
    }

    /* вычислить Y-пересечение с ребром
     * первый пиксель полностью внутри
     * функция потолка, если край находится сверху, в противном случае - пол
     * (+ y не работает)
     */
    int yIntersect(int xi, GeomEdge e){

        int y;
        double yfirst = (e.m) * (xi-0.5) + e.b;
        double ylast = (e.m) * (xi+0.5) + e.b;

        if (!e.isTop){
            y = (int)Math.floor(Math.min(yfirst, ylast));
        }
        else {
            y = (int)Math.ceil(Math.max(yfirst, ylast));
        }
        return y;
    }

    /* найти самый большой пиксель полностью внутри
     * просмотреть все края на предмет пересечения
     */
    int xIntersect(int y, Vector l){
        int x=0;
        double x0=0, x1=0;
        for(int i=0; i<this.size(); i++){
            GeomEdge e = (GeomEdge)l.elementAt(i);
            if (e.isRight && e.ymin <= y && e.ymax >= y){
                x0 = (y+0.5 - e.b) /e.m;
                x1 = (y-0.5 - e.b) /e.m;
            }
        }
        x = (int)Math.floor(Math.min(x0,x1));
        return x;
    }

    GeomEdge findEdge(int x, boolean isTop, Vector l){
        GeomEdge e,emax=(GeomEdge)l.elementAt(0);
        for (int i=0; i<this.size(); i++){
            e = (GeomEdge)l.elementAt(i);
            if (e.xmin == x){
                if (e.xmax != e.xmin){
                    if ((e.isTop && isTop)||(!e.isTop && !isTop)){
                        emax = e;
                    }
                }
            }

        }
        return emax;
    }

    /* вычислить 3 верхних и 3 нижних прямоугольника для каждого xi
     * найти самый большой прямоугольник с двумя углами
     */
    int computeLargestRectangle(){

        this.changed = false;
        Vector edgeList = computeEdgeList();
        this.RectList = new Vector();

        GeomEdge top, bottom;
        int ymax, ymin, xright, xlo, xhi;
        int area, maxArea = 0;
        int width, height, maxh=0, maxw=0;

        /* все прямоугольники с 2 и 3 углами наибольшего размера */
        int aAC=0,aBD=0,aABC=0,aABD=0,aACD=0,aBCD=0;
        GeomPoint pAC, pBD, pABC, pABD, pACD, pBCD;
        int hAC=0,wAC=0,hBD=0,wBD=0,hABC=0,wABC=0,hABD=0,wABD=0,hACD=0,wACD=0,hBCD=0,wBCD=0;
        boolean onA, onB, onC, onD;

        GeomPoint maxp = new GeomPoint(0,0);
        pAC = maxp; pBD = maxp; pABC = maxp; pABD = maxp; pACD = maxp; pBCD = maxp;

        Vector xint = new Vector();

        for(int i=0;i<this.ymax;i++){
            int x = xIntersect(i,edgeList);
            GeomPoint px = new GeomPoint(x,i);
            xint.add(px);
        }
        //найти первый верхний и нижний края
        top = findEdge(this.xmin, true, edgeList);
        bottom = findEdge(this.xmin, false, edgeList);

        //сканировать левое положение прямоугольника
        for(int xi=this.xmin; xi<this.xmax;xi++){

            ymin = yIntersect(xi, top);
            ymax = yIntersect(xi, bottom);

            for(int ylo = ymax;ylo>=ymin;ylo--){//ylo сверху вниз

                for(int yhi = ymin; yhi<=ymax; yhi++){

                    if (yhi>ylo){

                        onA = (yhi == ymax && !bottom.isRight);
                        onD = (ylo == ymin && !top.isRight);

                        xlo = (int)((GeomPoint)xint.elementAt(ylo)).x;//xIntersect(ylo,edgeList);
                        xhi = (int)((GeomPoint)xint.elementAt(yhi)).x;//xIntersect(yhi,edgeList);

                        xright = maxp.min(xlo,xhi);
                        onC = (xright == xlo && this.yxmax >= ylo);
                        onB = (xright == xhi && this.yxmax <= yhi);

                        height = yhi-ylo;
                        width = xright - xi;

                        if (!this.fixed){
                        }//!fixed
                        else{
                            int fixedWidth = (int)Math.ceil( ((double) height*this.fixedX)/((double)this.fixedY));
                            if (fixedWidth <= width){
                                width = fixedWidth;
                            }
                            else{
                                width = 0;
                            }
                        }
                        area = width * height;
                        //AC
                        if (onA && onC && !onB && !onD){
                            if (area > aAC){
                                aAC = area;
                                pAC = new GeomPoint(xi, ylo);
                                hAC = height;
                                wAC = width;
                            }
                        }
                        //BD
                        if (onB && onD && !onA && !onC){
                            if (area > aBD){
                                aBD = area;
                                pBD = new GeomPoint(xi, ylo);
                                hBD = height;
                                wBD = width;
                            }
                        }
                        //ABC
                        if (onA && onB && onC){
                            if (area > aABC){
                                aABC = area;
                                pABC = new GeomPoint(xi, ylo);
                                hABC = height;
                                wABC = width;
                            }
                        }
                        //ABD
                        if (onA && onB && onD){
                            if (area > aABD){
                                aABD = area;
                                pABD = new GeomPoint(xi, ylo);
                                hABD = height;
                                wABD = width;
                            }
                        }
                        //ACD
                        if (onA && onC && onD){
                            if (area > aACD){
                                aACD = area;
                                pACD = new GeomPoint(xi, ylo);
                                hACD = height;
                                wACD = width;
                            }
                        }
                        //BCD
                        if (onB && onC && onD){
                            if (area > aBCD){
                                aBCD = area;
                                pBCD = new GeomPoint(xi, ylo);
                                hBCD = height;
                                wBCD = width;
                            }
                        }

                        if(area>maxArea){
                            maxArea = area;
                            maxp = new GeomPoint(xi, ylo);
                            maxw = width;
                            maxh = height;
                        }
                    }
                }
            }
            if (xi == top.xmax){
                top = findEdge(xi,  true, edgeList);
            }
            if(xi == bottom.xmax){
                bottom = findEdge(xi, false, edgeList);
            }
        }
        this.rectp = maxp;
        this.recth = maxh;
        this.rectw = maxw;

        this.RectList.add(new Rectangle(pAC.x, pAC.y, wAC, hAC));
        this.RectList.add(new Rectangle(pBD.x, pBD.y, wBD, hBD));
        this.RectList.add(new Rectangle(pABC.x, pABC.y, wABC, hABC));
        this.RectList.add(new Rectangle(pABD.x, pABD.y, wABD, hABD));
        this.RectList.add(new Rectangle(pACD.x, pACD.y, wACD, hACD));
        this.RectList.add(new Rectangle(pBCD.x, pBCD.y, wBCD, hBCD));
        this.RectList.add(new Rectangle(maxp.x, maxp.y, maxw, maxh));
        return 0;
    }
}