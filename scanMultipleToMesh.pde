import processing.opengl.*;
import unlekker.util.*;
import unlekker.modelbuilder.*;
import SimpleOpenNI.*;


SimpleOpenNI kinect;
SimpleOpenNI kinect2;

boolean scanning = false;
int maxZ = 2000;
int spacing = 1;//Must be a factor of 640 or 480

UGeometry model;
UVertexList vertexList;

void setup()
{
  size(320*2+10, 480, OPENGL);
  
  SimpleOpenNI.start();
  
  
  kinect = new SimpleOpenNI(0,this);
  if(kinect.enableDepth() == false)
  {
    println("Camera One Failed");
    exit();
    return; 
  }
  
  kinect2 = new SimpleOpenNI(1,this);
  if(kinect2.enableDepth() == false)
  {
    println("Camera Two Failed");
    exit();
    return;     
  }
  
  model = new UGeometry();
  vertexList = new UVertexList();
}

void draw()
{
  background(0);
  
  SimpleOpenNI.updateAll();
  //kinect.update();
  
  translate(width/2, height/2, -1000);
  
  rotateX(radians(180));
  
  PVector[] depthPoints = kinect.depthMapRealWorld();
  PVector[] depthPoints90 = kinect2.depthMapRealWorld();
  
  
  depthPoints = cropScan(depthPoints);
  depthPoints90 = cropScan(depthPoints90);
  
  if(scanning)
  {
    createScan(depthPoints, false);
    createScan(depthPoints90, true);  
  }
        
  drawScan(depthPoints, 0);
  drawScan(depthPoints90, 2);

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
      || x == (640 - spacing))
      {
                
        PVector realWorld = new PVector();
        PVector projective = new PVector(x,y,maxZ);
        
        kinect.convertProjectiveToRealWorld(projective, realWorld);
        
        depthPoints[i] = realWorld;
      }
    }
  }
  
  return depthPoints;
}

void drawScan(PVector[] depthPoints, int window)
{
  for(int y = 0; y < 480 - spacing; y+=spacing)
  {
    for(int x=0; x < 640 - spacing; x+= spacing)
    {    
      
      if(!scanning)
      {

        stroke(255);
        int i = x + y*640;
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
        //model.translate(0, 0, -maxZ);
        
        
        int nw = x + y * 640;
        int ne = (x + spacing) + y * 640;
        int sw = x + (y + spacing)*640;
        int se = (x + spacing) + (y + spacing)*640;        
        
        model.addFace(new UVec3((int)depthPoints[nw].x,
                                (int)depthPoints[nw].y,
                                (int)depthPoints[nw].z),      
                      new UVec3((int)depthPoints[ne].x,
                                (int)depthPoints[ne].y,
                                (int)depthPoints[ne].z),
                      new UVec3((int)depthPoints[sw].x,
                                (int)depthPoints[sw].y,
                                (int)depthPoints[sw].z));
        model.addFace(new UVec3((int)depthPoints[ne].x,
                                (int)depthPoints[ne].y,
                                (int)depthPoints[ne].z),      
                      new UVec3((int)depthPoints[se].x,
                                (int)depthPoints[se].y,
                                (int)depthPoints[se].z),
                      new UVec3((int)depthPoints[sw].x,
                                (int)depthPoints[sw].y,
                                (int)depthPoints[sw].z));
      
    }
  }
  
      model.rotateY(radians(180));
    //model.toOrigin();
    
    model.endShape();
    
//    SimpleDateFormat logFileFmt = new SimpleDateFormat("'scan_'yyyMMddHHmmss'.stl'");
    model.writeSTL(this, "this.stl");
    
    scanning = false;
}
  
void keyPressed()
{
  
    println(maxZ);
    if(keyCode == UP)
    {
      maxZ += 100; 
    }
    if(keyCode == DOWN)
    {
       maxZ -= 100; 
    }
    if(key == ' ')
    {
      scanning = true;
    } 
}
