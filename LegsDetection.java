/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.israel.example;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.event.BasicEvent;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.graphics.FrameAnnotater;

import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 *
 * @author IsraelRuelas
 */
public class LegsDetection extends EventFilter2D implements FrameAnnotater 
{   
    //Point2D centroid = new Point2D.Double();
    ArrayList<Point2D> centroids = new ArrayList<>();
    Point2D current_point = new Point2D.Double();
    Point2D other_point = new Point2D.Double();
    double xsum = 0, ysum=0, mean_x=0, mean_y=0, variance_x=0, variance_y=0, std_x=0, std_y=0;
    double cxsum = 0, cysum=0, cmean_x=0, cmean_y=0, cvariance_x=0, cvariance_y=0, cstd_x=0, cstd_y=0;    
    
    @Override
    public void annotate(GLAutoDrawable drawable)      
    {
        //System.out.print("Centroids: " + centroids.size() + "\n"); 
        //System.out.print("Num_Centroids: " + centroids.size() + "Cvariance_x: "+ cvariance_x +"\n");     
        if (centroids.size()>=1 && cvariance_x<=150){
            System.out.print(cmean_x + " " + cmean_y +"\n");
            //System.out.print("Point here...." + centroids.get(0) + "\n"); 
            for (Point2D c: centroids){
                //System.out.print("Point centroid: " + c + "\n"); 
                GL2 gl3 = drawable.getGL().getGL2();
                gl3.glPushMatrix();
                gl3.glColor3f(0, 1, 0);
                gl3.glLineWidth(4);
                gl3.glBegin(GL2.GL_LINE_LOOP);
                gl3.glVertex2d(c.getX() - 2, c.getY() - 2);
                gl3.glVertex2d(c.getX() + 2, c.getY() - 2);
                gl3.glVertex2d(c.getX() + 2, c.getY() + 2);
                gl3.glVertex2d(c.getX() - 2, c.getY() + 2);
                gl3.glEnd();
                gl3.glPopMatrix();
            }
        }
        
        //Ptinting the cmean_x
        GL2 gl4 = drawable.getGL().getGL2();
        gl4.glColor3f(1, 0, 0);
        gl4.glLineWidth(4);
        gl4.glBegin(GL2.GL_LINE_LOOP);
        gl4.glVertex2d(cmean_x, cmean_y-20);
        gl4.glVertex2d(cmean_x, cmean_y+20);
        gl4.glEnd();
        
        cxsum=0;
        cysum=0;
        xsum = 0;
        ysum=0;
        centroids.clear();

    }    
    
    public LegsDetection(AEChip chip)
    {
        super(chip);
    }
    
    @Override
    public EventPacket<?> filterPacket(EventPacket<?> in)
    {        
        if(in.isEmpty()){ 
            return in;
        }
        /////////////////// Getting the size of the package of events (number of events)  /////////////////////////////
        int n = in.getSize();
        if(n<1000){
            return in;
        }
        //System.out.print("---------------------------" + n + "---------------------------\n");
        ////////////////// Generating an array with all the pixels/////////////////////
        ArrayList<ArrayList<Double>> all_pixels = new ArrayList<ArrayList<Double>>();
        //double xsum = 0, ysum=0, mean_x=0, mean_y=0, variance_x=0, variance_y=0, std_x=0, std_y=0, center_of_mass_x = 0, center_of_mass_y=0;
        
        for (Object o : in) {
            BasicEvent e = (BasicEvent) o;
            ArrayList<Double> pixel = new ArrayList<>();
            pixel.add((double)(e.x));
            pixel.add((double)(e.y));
            xsum+=(double)(e.x);
            ysum+=(double)(e.y);
            
            if (e.isSpecial() || all_pixels.contains(pixel)){
                continue;
            }
            all_pixels.add(pixel);   
        }
        
        //Getting mean, variance ans std_dev for x and y
        mean_x=xsum/n;
        mean_y=ysum/n;
        
        double temp_x=0;
        double temp_y=0;
        
        for(ArrayList<Double> a :all_pixels){ 
            temp_x += (mean_x-a.get(0))*(mean_x-a.get(0));
            temp_y += (mean_y-a.get(1))*(mean_y-a.get(1));
        }
        variance_x=temp_x/n;
        variance_y=temp_y/n;
        
        std_x=Math.sqrt(variance_x);
        std_y=Math.sqrt(variance_y);
        
        // Getting pixels below mean_x for legs identification
        ArrayList<ArrayList<Double>> new_pixels = new ArrayList<ArrayList<Double>>();
        
        for(ArrayList<Double> px: all_pixels){
            if(px.get(1)<=mean_y){
                ArrayList<Double> new_px = new ArrayList<Double>();
                new_px.add(px.get(0));
                new_px.add(px.get(1));
                new_pixels.add(new_px);
            }
        }
        
        ArrayList<ArrayList<ArrayList<Double>>> body_parts = new ArrayList<ArrayList<ArrayList<Double>>>();
        ArrayList<ArrayList<Double>> used_pixels = new ArrayList<ArrayList<Double>>();
                
        for (ArrayList<Double> current_px: new_pixels){
            ArrayList<ArrayList<Double>> part_pixels = new ArrayList<ArrayList<Double>>();
            for (ArrayList<Double> other_px: new_pixels){
                if(used_pixels.contains(other_px)){
                    continue;
                }
                current_point.setLocation(current_px.get(0),current_px.get(1)); 
                other_point.setLocation(other_px.get(0),other_px.get(1)); 
                double distance = current_point.distance(other_point);
                //System.out.print("Point: "+current_px+"  Other Point: "+other_px+"  Distance: "+distance+"\n");
                if(distance<= (double)(MinDistanceBetweenPoints)){
                    used_pixels.add(other_px);
                    part_pixels.add(other_px);
                } 
            }
            if(part_pixels.size()>=MinPixelsPerGroup){
                body_parts.add(part_pixels);
            }
            //System.out.print("Part: "+part+"\n");    
        }
        used_pixels.clear();
        
        ///////////////// Perform the separation of the events into groups (distance based) /////////////////
        for (ArrayList<ArrayList<Double>> pp: body_parts){
            double x_sum = 0, y_sum = 0, center_of_mass_x = 0, center_of_mass_y = 0;
            Point2D centroid = new Point2D.Double();
            for (ArrayList<Double> px: pp){
                x_sum += px.get(0);
                y_sum += px.get(1);
            }    
            center_of_mass_x = x_sum/pp.size();
            center_of_mass_y = y_sum/pp.size();
            centroid.setLocation(center_of_mass_x,center_of_mass_y);
            centroids.add(centroid);
        }
           
        for (Point2D c: centroids){
            cxsum+=c.getX();
            cysum+=c.getY();
        }
        cmean_x=cxsum/centroids.size();
        cmean_y=cysum/centroids.size();
        
        double temp_cx=0;
        double temp_cy=0;
        
        for(Point2D a :centroids){ 
            temp_cx += (cmean_x-a.getX())*(cmean_x-a.getX());
            temp_cy += (cmean_y-a.getY())*(cmean_y-a.getY());
        }
        cvariance_x=temp_cx/centroids.size();
        cstd_x=Math.sqrt(cvariance_x);
        
        cvariance_y=temp_cy/centroids.size();
        cstd_y=Math.sqrt(cvariance_y);
        
        return in;
    }
    
    @Override
    public void resetFilter()
    {
        
        //things to do in the filter reset
        //center_of_mass.setLocation(chip.getSizeX() / 2, chip.getSizeY() / 2);
        //ArrayList<Point2D> centroids = new ArrayList<>();
    
        
        
               
    }
    
    @Override
    public void initFilter()
    {
        //things to do in the filter init
     
        
        
    }
   
    protected int MinPixelsPerGroup= getPrefs().getInt("LegsDetection.MinPixelsPerGroup",7);
    {
        setPropertyTooltip("MinPixelsPerGroup","Minimum number of pixels per group");
    }
    
    public int getMinPixelsPerGroup()
    {
        return this.MinPixelsPerGroup;
    
    }
    
    public void setMinPixelsPerGroup(final int MinPixelsPerGroup)
    {
        getPrefs().putInt("LegsDetection.MinPixelsPerGroup", MinPixelsPerGroup);
        getSupport().firePropertyChange("MinPixelsPerGroup", this.MinPixelsPerGroup,MinPixelsPerGroup);
        this.MinPixelsPerGroup = MinPixelsPerGroup;
    }

   
    protected int MinDistanceBetweenPoints= getPrefs().getInt("LegsDetection.MinDistanceBetweenPoints",4);
    {
        setPropertyTooltip("MinDistanceBetweenPoints","Minimum distance between points");
    }
    
    public int getMinDistanceBetweenPoints()
    {
        return this.MinDistanceBetweenPoints;
    
    }
    
    public void setMinDistanceBetweenPoints(final int MinDistanceBetweenPoints)
    {
        getPrefs().putInt("LegsDetection.MinDistanceBetweenPoints", MinDistanceBetweenPoints);
        getSupport().firePropertyChange("MinDistanceBetweenPoints", this.MinDistanceBetweenPoints,MinDistanceBetweenPoints);
        this.MinDistanceBetweenPoints = MinDistanceBetweenPoints;
    }
}

