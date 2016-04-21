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
import net.sf.jaer.graphics.MultilineAnnotationTextRenderer;
import java.awt.Color;

import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 *
 * @author IsraelRuelas
 */
public class HumanDetection_v4 extends EventFilter2D implements FrameAnnotater 
{   
    //Point2D centroid = new Point2D.Double();
    ArrayList<Point2D> head_centroids = new ArrayList<>();
    ArrayList<Point2D> legs_centroids = new ArrayList<>();
    
    Point2D current_point = new Point2D.Double();
    Point2D other_point = new Point2D.Double();
    double xsum = 0, ysum=0, mean_x=0, mean_y=0, variance_x=0, variance_y=0, std_x=0, std_y=0;
    double head_cxsum = 0, head_cysum=0, head_cmean_x=0, head_cmean_y=0, head_cvariance_x=0, head_cvariance_y=0, head_cstd_x=0, head_cstd_y=0;    
    double legs_cxsum = 0, legs_cysum=0, legs_cmean_x=0, legs_cmean_y=0, legs_cvariance_x=0, legs_cvariance_y=0, legs_cstd_x=0, legs_cstd_y=0;    
        
    @Override
    public void annotate(GLAutoDrawable drawable)      
    {
        MultilineAnnotationTextRenderer.setColor(Color.CYAN);
        MultilineAnnotationTextRenderer.resetToYPositionPixels(chip.getSizeY() * .9f);
        MultilineAnnotationTextRenderer.setScale(.3f);
        
        //This is the total centroid
        GL2 gl5 = drawable.getGL().getGL2();
        gl5.glColor3f(0, 0, 1);
        gl5.glLineWidth(4);
        gl5.glBegin(GL2.GL_LINE_LOOP);
        gl5.glVertex2d(mean_x, mean_y-10);
        gl5.glVertex2d(mean_x, mean_y+10);
        gl5.glEnd();

        if (head_centroids.size()>=1 && legs_centroids.size()>=2 && legs_cstd_x>7){
            MultilineAnnotationTextRenderer.renderMultilineString("HUMAN!!!");
            //printing head centroids    
            for (Point2D c: head_centroids){
                //System.out.print("Point centroid: " + c + "\n"); 
                GL2 gl1 = drawable.getGL().getGL2();
                gl1.glPushMatrix();
                gl1.glColor3f(0, 1, 0);
                gl1.glLineWidth(4);
                gl1.glBegin(GL2.GL_LINE_LOOP);
                gl1.glVertex2d(c.getX() - 2, c.getY() - 2);
                gl1.glVertex2d(c.getX() + 2, c.getY() - 2);
                gl1.glVertex2d(c.getX() + 2, c.getY() + 2);
                gl1.glVertex2d(c.getX() - 2, c.getY() + 2);
                gl1.glEnd();
                gl1.glPopMatrix();
            }
            //Ptinting the head_cmean_x
            GL2 gl2 = drawable.getGL().getGL2();
            gl2.glColor3f(0, 1, 0);
            gl2.glLineWidth(4);
            gl2.glBegin(GL2.GL_LINE_LOOP);
            gl2.glVertex2d(head_cmean_x-head_cstd_x, head_cmean_y-head_cstd_y);
            gl2.glVertex2d(head_cmean_x+head_cstd_x, head_cmean_y-head_cstd_y);
            gl2.glVertex2d(head_cmean_x+head_cstd_x, head_cmean_y+head_cstd_y);
            gl2.glVertex2d(head_cmean_x-head_cstd_x, head_cmean_y+head_cstd_y);
            gl2.glEnd();
            head_centroids.clear();
            //printing leg centroids
            for (Point2D c: legs_centroids){
                //System.out.print("Point centroid: " + c + "\n"); 
                GL2 gl3 = drawable.getGL().getGL2();
                gl3.glPushMatrix();
                gl3.glColor3f(1, 0, 0);
                gl3.glLineWidth(4);
                gl3.glBegin(GL2.GL_LINE_LOOP);
                gl3.glVertex2d(c.getX() - 2, c.getY() - 2);
                gl3.glVertex2d(c.getX() + 2, c.getY() - 2);
                gl3.glVertex2d(c.getX() + 2, c.getY() + 2);
                gl3.glVertex2d(c.getX() - 2, c.getY() + 2);
                gl3.glEnd();
                gl3.glPopMatrix();
            }
            GL2 gl6 = drawable.getGL().getGL2();
            gl6.glColor3f(1, 0, 1);
            gl6.glLineWidth(4);
            gl6.glBegin(GL2.GL_LINE_LOOP);
            gl6.glVertex2d(legs_cmean_x-legs_cstd_x, legs_cmean_y-legs_cstd_y);
            gl6.glVertex2d(legs_cmean_x+legs_cstd_x, legs_cmean_y-legs_cstd_y);
            gl6.glVertex2d(legs_cmean_x+legs_cstd_x, legs_cmean_y+legs_cstd_y);
            gl6.glVertex2d(legs_cmean_x-legs_cstd_x, legs_cmean_y+legs_cstd_y);
            gl6.glEnd();
            
        }
        //else{
        //   MultilineAnnotationTextRenderer.renderMultilineString("Waiting...");    
        //}
               
        head_centroids.clear();
        legs_centroids.clear();
        legs_cvariance_x=0;
        legs_cvariance_y=0;
        
    }    
    
    public HumanDetection_v4(AEChip chip)
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
        //if(n<1000){
        //    return in;
        //}
        //System.out.print("---------------------------" + n + "---------------------------\n");
        ////////////////// Generating an array with all the pixels/////////////////////
        ArrayList<ArrayList<Double>> all_pixels = new ArrayList<ArrayList<Double>>();
        xsum = 0;
        ysum=0;
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
        for(ArrayList<Double> px :all_pixels){ 
            variance_x += (mean_x-px.get(0))*(mean_x-px.get(0));
            variance_y += (mean_y-px.get(1))*(mean_y-px.get(1));
        }
        variance_x=variance_x/n;
        variance_y=variance_y/n;
        std_x=Math.sqrt(variance_x);
        std_y=Math.sqrt(variance_y);
        
        //Separating pixels for head and for leg detection
        ArrayList<ArrayList<Double>> up_pixels = new ArrayList<ArrayList<Double>>();
        ArrayList<ArrayList<Double>> down_pixels = new ArrayList<ArrayList<Double>>();
        for(ArrayList<Double> px :all_pixels){ 
            if(px.get(1)>=mean_y+std_y){
                ArrayList<Double> h_px = new ArrayList<Double>();
                h_px.add(px.get(0));
                h_px.add(px.get(1));
                up_pixels.add(h_px);
            }
            else if (px.get(1)<=mean_y-std_y){
                ArrayList<Double> l_px = new ArrayList<Double>();
                l_px.add(px.get(0));
                l_px.add(px.get(1));
                down_pixels.add(l_px);
            }
        }
        //System.out.print("Down pixels: "+down_pixels.size()+"\n");
        
        ////////////////////////// Detecting Head
        ArrayList<ArrayList<Double>> used_pixels = new ArrayList<ArrayList<Double>>();
        for (ArrayList<Double> current_px: up_pixels){
            ArrayList<ArrayList<Double>> part_pixels = new ArrayList<ArrayList<Double>>();
            for (ArrayList<Double> other_px: up_pixels){
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
                //body_parts.add(part_pixels);
                double center_of_mass_x = 0, center_of_mass_y = 0;
                Point2D centroid = new Point2D.Double();
                for (ArrayList<Double> px: part_pixels){
                    center_of_mass_x += px.get(0);
                    center_of_mass_y += px.get(1);
                }
                center_of_mass_x = center_of_mass_x/part_pixels.size();
                center_of_mass_y = center_of_mass_y/part_pixels.size();
                centroid.setLocation(center_of_mass_x,center_of_mass_y);
                head_centroids.add(centroid);
                head_cxsum+=centroid.getX();
                head_cysum+=centroid.getY();
            }
            //System.out.print("Part: "+part+"\n");    
        }
        //System.out.print("Number of head centroids: "+head_centroids.size()+"\n");    
        used_pixels.clear();
        head_cmean_x=head_cxsum/head_centroids.size();
        head_cmean_y=head_cysum/head_centroids.size();
        for(Point2D a :head_centroids){ 
            head_cvariance_x += (head_cmean_x-a.getX())*(head_cmean_x-a.getX());
            head_cvariance_y += (head_cmean_y-a.getY())*(head_cmean_y-a.getY());
        }
        head_cvariance_x=head_cvariance_x/head_centroids.size();
        head_cstd_x=Math.sqrt(head_cvariance_x);
        head_cvariance_y=head_cvariance_y/head_centroids.size();
        head_cstd_y=Math.sqrt(head_cvariance_y);
        
        head_cxsum=0;
        head_cysum=0;
        head_cvariance_x=0;
        head_cvariance_y=0;
        
        ////////////////////////// Detecting Legs
        for (ArrayList<Double> current_px: down_pixels){
            ArrayList<ArrayList<Double>> part_pixels = new ArrayList<ArrayList<Double>>();
            for (ArrayList<Double> other_px: down_pixels){
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
                //body_parts.add(part_pixels);
                double center_of_mass_x = 0, center_of_mass_y = 0;
                Point2D centroid = new Point2D.Double();
                for (ArrayList<Double> px: part_pixels){
                    center_of_mass_x += px.get(0);
                    center_of_mass_y += px.get(1);
                }
                center_of_mass_x = center_of_mass_x/part_pixels.size();
                center_of_mass_y = center_of_mass_y/part_pixels.size();
                centroid.setLocation(center_of_mass_x,center_of_mass_y);
                legs_centroids.add(centroid);
                legs_cxsum+=centroid.getX();
                legs_cysum+=centroid.getY();
            }
            //System.out.print("Part: "+part+"\n");    
        }
        used_pixels.clear();
        //System.out.print("Number of Leg centroids: "+legs_centroids.size()+"\n");    
     
        legs_cmean_x=legs_cxsum/legs_centroids.size();
        legs_cmean_y=legs_cysum/legs_centroids.size();
        for(Point2D a :legs_centroids){ 
            legs_cvariance_x += (legs_cmean_x-a.getX())*(legs_cmean_x-a.getX());
            legs_cvariance_y += (legs_cmean_y-a.getY())*(legs_cmean_y-a.getY());
        }
        legs_cvariance_x=legs_cvariance_x/legs_centroids.size();
        legs_cstd_x=Math.sqrt(legs_cvariance_x);
        legs_cvariance_y=legs_cvariance_y/legs_centroids.size();
        legs_cstd_y=Math.sqrt(legs_cvariance_y);
        
        legs_cxsum=0;
        legs_cysum=0;

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
   
    protected int MinPixelsPerGroup= getPrefs().getInt("HumanDetection_v4.MinPixelsPerGroup",7);
    {
        setPropertyTooltip("MinPixelsPerGroup","Minimum number of pixels per group");
    }
    
    public int getMinPixelsPerGroup()
    {
        return this.MinPixelsPerGroup;
    
    }
    
    public void setMinPixelsPerGroup(final int MinPixelsPerGroup)
    {
        getPrefs().putInt("HumanDetection_v4.MinPixelsPerGroup", MinPixelsPerGroup);
        getSupport().firePropertyChange("MinPixelsPerGroup", this.MinPixelsPerGroup,MinPixelsPerGroup);
        this.MinPixelsPerGroup = MinPixelsPerGroup;
    }

   
    protected int MinDistanceBetweenPoints= getPrefs().getInt("HumanDetection_v4.MinDistanceBetweenPoints",4);
    {
        setPropertyTooltip("MinDistanceBetweenPoints","Minimum distance between points");
    }
    
    public int getMinDistanceBetweenPoints()
    {
        return this.MinDistanceBetweenPoints;
    
    }
    
    public void setMinDistanceBetweenPoints(final int MinDistanceBetweenPoints)
    {
        getPrefs().putInt("HumanDetection_v4.MinDistanceBetweenPoints", MinDistanceBetweenPoints);
        getSupport().firePropertyChange("MinDistanceBetweenPoints", this.MinDistanceBetweenPoints,MinDistanceBetweenPoints);
        this.MinDistanceBetweenPoints = MinDistanceBetweenPoints;
    }
}

