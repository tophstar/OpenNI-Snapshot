package scanMultipleToMesh;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import unlekker.modelbuilder.*;
import SimpleOpenNI.*;

public class scanMultipleToMesh extends PApplet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	SimpleOpenNI kinect;
	SimpleOpenNI kinect2;

	boolean scanning = false;
	int maxZ = 2000;
	int spacing = 1;//Must be a factor of 640 or 480
	int margin = 0;
	int distance = 1130;//1061;
	
	int innerAngle = -27;
	
	int scanCount = 0;
	int totalScanCount = 0;

	int invalidZ = 99999;
	int zDiffRange = 10;
	
	UGeometry model;
	UVertexList vertexList;

	int time;
	int waitTime = 3000;
	
	public void setup()
	{
		time = millis();
		
	  size(320*2+10, 480, OPENGL);
	  
	  SimpleOpenNI.start();
	  
	  
	  kinect = new SimpleOpenNI(0,this);
	  if(kinect.enableDepth() == false)
	  {
	    println("Camera One Failed");
	    exit();
	    return; 
	  }
	  else
	  {
		  kinect.enableRGB();
		  kinect.alternativeViewPointDepthToImage();
	  }
	  
	  kinect2 = new SimpleOpenNI(1,this);
	  if(kinect2.enableDepth() == false)
	  {
	    println("Camera Two Failed");
	    exit();
	    return;     
	  }
	  else
	  {
		  kinect2.enableRGB();
		  kinect2.alternativeViewPointDepthToImage();
	  }
	  
	  model = new UGeometry();
	  vertexList = new UVertexList();
	}

	public void draw()
	{
	  if(scanCount < 1)
	  {
	  background(0);
	  
	  SimpleOpenNI.updateAll();
	  //kinect.update();
	  
	  translate(width/2, height/2, -1000);
	  
	  rotateX(radians(180));
	  
	  PVector[] depthPoints = kinect.depthMapRealWorld();
	  PVector[] depthPoints90 = kinect2.depthMapRealWorld();


		  if(scanning)
		  {
			scanCount = 1;
			
			totalScanCount++;
			  
		    depthPoints = cropScan(depthPoints);
		    depthPoints90 = cropScan(depthPoints90);
		    
		    if(totalScanCount%2 == 0)
		    {
			    createScan(depthPoints, false);
			    createScan(depthPoints90, true);
		    }
		    else
		    {
			    createScan(depthPoints90, true);
			    createScan(depthPoints, false);
		    }
			
		    //Draw one point
	        stroke(255);
	        point(1, 1, 1);
		  }
		  else
		  {  
		    drawScan(kinect, depthPoints, 0);
		    drawScan(kinect2, depthPoints90, 2);
		  }
	  }
	  else
	  {
	     kinect = new SimpleOpenNI(0, this);
         kinect.enableDepth();
		 kinect.enableRGB();
		 kinect.alternativeViewPointDepthToImage();
         
	     kinect2 = new SimpleOpenNI(1, this);
         kinect2.enableDepth();
		 kinect2.enableRGB();
		 kinect2.alternativeViewPointDepthToImage();
         
		 scanCount -= 1;
	  }
	}

	PVector[] cropScan(PVector[] depthPoints)
	{
	  for(int y=0; y <= (480 - spacing); y+=spacing)
	  {
	    for(int x=0; x <= (640 - spacing); x+= spacing)
	    {
	      
	      int i = y*640+x;
	      PVector p = depthPoints[i];
	      
	      if(
	      p.z < 10 
	      || p.z > maxZ 
	      || y == 0 
	      || y == (480 - spacing) 
	      || x == 0 
	      || x == (640 - spacing)
	      || x < margin
	      || x > (640 - margin)
	      )
	      {
	                
	        PVector realWorld = new PVector();
	        PVector projective = new PVector(x,y,invalidZ);
	        
	        kinect.convertProjectiveToRealWorld(projective, realWorld); 
	               
	        depthPoints[i] = realWorld;
	      }
	    }
	  }
	  
	  return depthPoints;
	}

	void drawScan(SimpleOpenNI thisKinect,PVector[] depthPoints, int window)
	{
	  PImage rgbImage = thisKinect.rgbImage();
		
	  for(int y = 0; y < 480 - spacing; y+=spacing)
	  {
	    for(int x=0; x < 640 - spacing; x+= spacing)
	    {    
	      
	      if(!scanning)
	      {

	        int i = x + y*640;
	        stroke(rgbImage.pixels[i]);

	        PVector currentPoint = depthPoints[i];
	        point(currentPoint.x-1400*(1-window), currentPoint.y, currentPoint.z);
	      }
	      
	    }
	  }
	}

	void createScan(PVector[] depthPoints, boolean flip)
	{
	  model.beginShape(TRIANGLES);


	        
	  for(int y = 0; y < 480 - spacing; y+=spacing)
	  {
	    for(int x=0; x < 640 - spacing; x+= spacing)
	    {
	        
	        int nw = x + y * 640;
	        int ne = (x + spacing) + y * 640;
	        int sw = x + (y + spacing)*640;
	        int se = (x + spacing) + (y + spacing)*640;        

	        if(depthPoints[nw].z == invalidZ &&
	        depthPoints[ne].z == invalidZ &&
	        depthPoints[sw].z == invalidZ &&
	        depthPoints[se].z == invalidZ)
	        {
	        	//do nothing if none of the points are valid
	        }
	        else if(depthPoints[nw].z != invalidZ)
	        {
	        	/*
	        	 * Make sure that all the z points are close enough togeather to make a nice surface
	        	 */
	        	boolean zValuesInRange = false;
	        	float[] zPoints = new float[5];
	        	zPoints[0] = depthPoints[nw].z;
	        	zPoints[1] = depthPoints[ne].z;
	        	zPoints[2] = depthPoints[sw].z;
	        	zPoints[3] = depthPoints[se].z;
	        	zPoints[4] = depthPoints[sw].z;
	        	
	        	for(int i = 0; i < 5; i++)
	        	{
	        		for(int k = i+1; k < 5; k++)
	        		{
	        			if(zPoints[i]-zPoints[k] < zDiffRange)
	        			{
	        				zValuesInRange = true;
	        			}
	        		}
	        	}
	        	
		        if(
		        zValuesInRange &&		
		        depthPoints[nw].z != invalidZ &&
		        depthPoints[ne].z != invalidZ &&
		        depthPoints[sw].z != invalidZ &&
		        depthPoints[se].z != invalidZ)
		        {
		        //create a normal face
		          model.addFace(new UVec3((int)depthPoints[nw].x,
		                                  (int)depthPoints[nw].y,
		                                  (int)depthPoints[nw].z-distance),      
		                        new UVec3((int)depthPoints[ne].x,
		                                  (int)depthPoints[ne].y,
		                                  (int)depthPoints[ne].z-distance),
		                        new UVec3((int)depthPoints[sw].x,
		                                  (int)depthPoints[sw].y,
		                                  (int)depthPoints[sw].z-distance));
		          model.addFace(new UVec3((int)depthPoints[ne].x,
		                                  (int)depthPoints[ne].y,
		                                  (int)depthPoints[ne].z-distance),      
		                        new UVec3((int)depthPoints[se].x,
		                                  (int)depthPoints[se].y,
		                                  (int)depthPoints[se].z-distance),
		                        new UVec3((int)depthPoints[sw].x,
		                                  (int)depthPoints[sw].y,
		                                  (int)depthPoints[sw].z-distance));
		        }
	        }
	    }
	  }
	  
	    if(flip)
	    {
	      model.rotateY(radians(180));
	      model.rotateX(radians(2*innerAngle));
	    }
	    //model.toOrigin();
	    
	    model.endShape();
	    
	    java.util.Date d = new java.util.Date();
	    
//	    SimpleDateFormat logFileFmt = new SimpleDateFormat("'scan_'yyyMMddHHmmss'.stl'");
	    model.writeSTL(this, "this_" + d.getTime() +".stl");
	    
	    
	    model.reset();
	    scanning = false;
	}
	  
	public void keyPressed()
	{
	  
	    
	    if(keyCode == UP)
	    {
	      maxZ += 25;
	      distance += 25; 
	      println(maxZ);
	    }
	    else if(keyCode == DOWN)
	    {
	      maxZ -= 25;
	      distance -= 25; 
	      println(maxZ);
	    }
	    else if(keyCode == RIGHT)
	    {
	      distance += 100;
	      println(distance);
	    }
	    else if(keyCode == LEFT)
	    {
	      distance -= 100; 
	      println(distance);
	    }
	    else if(key == ' ')
	    {
	    	time = millis();
	    	while(millis() - time < waitTime)
	    	{
	    		println("Waiting "+ time + " " + millis());
	    	}
	      scanning = true;
	    } 
	}
}
