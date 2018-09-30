package project1;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import graphicslib3D.Matrix3D;

import javax.swing.*;
import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static graphicslib3D.GLSLUtils.readShaderSource;

/**
 * CS-4613 Project 1 - Koch Snowflake
 * <p>
 * Based on Dr. Mauricio Papa's Sierpinski gasket code.
 *
 * @author Eric Peterson
 */
public class Project1 extends JFrame implements GLEventListener
{
	/*
	 * Member Variables
	 */
	private GLCanvas m_myCanvas;
	private int m_renderingProgram;
	private int m_vao[] = new int[1];
	private int m_vbo[] = new int[2];
	private float m_cameraX, m_cameraY, m_cameraZ;
	private int m_n; // Recursion level
	private float m_sideLength;
	private float[] m_vertexPositions = new float[2 * 2]; // Two points, two coordinates
	
	public Project1(int n, float sideLength)
	{
		m_n = n;
		m_sideLength = sideLength;
		for(int i = 0; i <= m_n; ++i)
		{
			displayCalculations(i);
		}
		setTitle("Project 1 - Koch Snowflake");
		//setSize((int) (300 * m_sideLength), (int) (300 * m_sideLength));
		setSize(600, 600);
		// Making sure we get a GL4 context for the canvas
		GLProfile profile = GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities = new GLCapabilities(profile);
		m_myCanvas = new GLCanvas(capabilities);
		// end GL4 context
		m_myCanvas.addGLEventListener(this);
		getContentPane().add(m_myCanvas);
		this.setVisible(true);
	}
	
	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		// Define the triangle
		float[] v1 = new float[2]; // Two coordinates
		float[] v2 = new float[2]; // Two coordinates
		float[] v3 = new float[2]; // Two coordinates
		// The first three vertices define the starting triangle
		// Equilateral triangle centered at the origin
		// Top vertex - x and y
		v1[0] = 0;
		v1[1] = m_sideLength * (float) Math.sqrt(3) / 3;
		// Bottom left
		v2[0] = -0.5f * m_sideLength;
		v2[1] = -(float) Math.sqrt(3) * m_sideLength / 6;
		// Bottom right
		v3[0] = 0.5f * m_sideLength;
		v3[1] = -(float) Math.sqrt(3) * m_sideLength / 6;
		// Done defining triangle
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(m_renderingProgram);
		
		int mv_loc = gl.glGetUniformLocation(m_renderingProgram, "mv_matrix");
		int proj_loc = gl.glGetUniformLocation(m_renderingProgram, "proj_matrix");
		
		float aspect = (float) m_myCanvas.getWidth() / (float) m_myCanvas.getHeight();
		Matrix3D pMat = orthogonal(-1.5f, 1.5f, 1.5f, -1.5f, 0.1f, 1000.0f);
		
		Matrix3D vMat = new Matrix3D();
		vMat.translate(-m_cameraX, -m_cameraY, -m_cameraZ);
		// Just drawing 2D - not moving the object
		Matrix3D mMat = new Matrix3D();
		mMat.setToIdentity();
		
		Matrix3D mvMat = new Matrix3D();
		mvMat.concatenate(vMat);
		mvMat.concatenate(mMat);
		
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvMat.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);
		
		//gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[0]);
		gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0); // We are only passing two components
		gl.glEnableVertexAttribArray(0);
		
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		koch(v1, v2, v3);
	}
	
	public void init(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) drawable.getGL();
		m_renderingProgram = createShaderProgram();
		m_cameraX = 0.0f;
		m_cameraY = 0.0f;
		m_cameraZ = 3.0f;
		gl.glGenVertexArrays(m_vao.length, m_vao, 0);
		gl.glBindVertexArray(m_vao[0]);
		gl.glGenBuffers(m_vbo.length, m_vbo, 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, m_vbo[0]);
		
	}
	
	private Matrix3D perspective(float fovy, float aspect, float n, float f)
	{
		float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		r.setElementAt(0, 0, A);
		r.setElementAt(1, 1, q);
		r.setElementAt(2, 2, B);
		r.setElementAt(3, 2, -1.0f);
		r.setElementAt(2, 3, C);
		r.setElementAt(3, 3, 0.0f);
		return r;
	}
	
	private Matrix3D orthogonal(float left, float right, float top, float bottom, float near, float far)
	{
		Matrix3D r = new Matrix3D();
		r.setElementAt(0, 0, 2.0 / (right - left));
		r.setElementAt(1, 1, 2.0 / (top - bottom));
		r.setElementAt(2, 2, 1 / (far - near));
		r.setElementAt(3, 3, 1.0f);
		r.setElementAt(0, 3, -(right + left) / (right - left));
		r.setElementAt(1, 3, -(top + bottom) / (top - bottom));
		r.setElementAt(2, 3, -near / (far - near));
		return r;
	}
	
	public static void main(String[] args)
	{
		int n = 0;
		float sideLength = 2.0f;
		boolean tryAgain;
		do
		{
			try
			{
				tryAgain = false;
				String response = JOptionPane.showInputDialog(null, "Enter in the number of recursion levels: ", "Koch Snowflake", JOptionPane.QUESTION_MESSAGE);
				n = Integer.parseInt(response);
			}
			catch(NumberFormatException e)
			{
				tryAgain = true;
			}
		} while(tryAgain);
		do
		{
			try
			{
				tryAgain = false;
				String response = JOptionPane.showInputDialog(null, "Enter in the side length of the original triangle: ", "Koch Snowflake", JOptionPane.QUESTION_MESSAGE);
				sideLength = Float.parseFloat(response);
			}
			catch(NumberFormatException e)
			{
				tryAgain = true;
			}
		} while(tryAgain);
		new Project1(n, sideLength);
	}
	
	private void displayCalculations(int n)
	{
		int sides = 3 * (int) Math.pow(4, n);
		double effectiveSideLength = m_sideLength / Math.pow(3, n);
		double perimeter = sides * effectiveSideLength;
		double heightOfOriginalTriangle = m_sideLength * Math.tan(Math.toRadians(60)) / 2; // tan(60) = h / (b / 2)
		double areaOfOriginalTriangle = m_sideLength * (heightOfOriginalTriangle) / 2; // A = b * h / 2;
		double area = (areaOfOriginalTriangle / 5) * (8 - (3 * Math.pow((double) 4 / 9, n))); // Thank you Wikipedia.
		
		System.out.println("n = " + n);
		System.out.println("Sides = " + sides);
		System.out.println("Perimeter = " + perimeter);
		System.out.println("Area = " + area);
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
	}
	
	public void dispose(GLAutoDrawable drawable)
	{
	}
	
	private int createShaderProgram()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		String vshaderSource[] = readShaderSource("shaders/vert.shader");
		String fshaderSource[] = readShaderSource("shaders/frag.shader");
		
		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);
		
		gl.glCompileShader(vShader);
		gl.glCompileShader(fShader);
		
		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		gl.glLinkProgram(vfprogram);
		return vfprogram;
	}
	
	private void koch(float[] v1, float[] v2, float[] v3)
	{
		processLine(v1, v2, m_n);
		processLine(v2, v3, m_n);
		processLine(v3, v1, m_n);
	}
	
	// Processing triangles
	private void processLine(float[] v1, float[] v2, int n)
	{
		if(n > 0) // Recurse
		{
			// Points of new triangle.
			float[] top = new float[2];
			float[] left = new float[2];
			float[] right = new float[2];
			
			top[0] = ((float) 1 / 2) * (v2[0] + v1[0]) + ((float) Math.sqrt(3) / 6) * (v2[1] - v1[1]);
			top[1] = ((float) 1 / 2) * (v2[1] + v1[1]) - ((float) Math.sqrt(3) / 6) * (v2[0] - v1[0]);
			
			for(int i = 0; i < left.length; ++i)
			{
				left[i] = v1[i] + ((float) 1 / 3) * (v2[i] - v1[i]);
				right[i] = v2[i] - ((float) 1 / 3) * (v2[i] - v1[i]);
			}
			
			// Recurse
			processLine(v1, left, n - 1);
			processLine(left, top, n - 1);
			processLine(top, right, n - 1);
			processLine(right, v2, n - 1);
			
			/*// Bottom Triangle (only seen in iteration 1)
			
			float[] b1 = new float[2]; // Top Point
			float[] b2 = new float[2]; // Left Point
			float[] b3 = new float[2]; // Right Point
			
			b1[0] = ((float) 1 / 2) * (v3[0] + v2[0]) + ((float) Math.sqrt(3) / 6) * (v3[1] - v2[1]);
			b1[1] = ((float) 1 / 2) * (v3[1] + v2[1]) + ((float) Math.sqrt(3) / 6) * (v2[0] - v3[0]);
			
			b2[0] = v3[0] + ((float) 1 / 3) * (v2[0] - v3[0]);
			b2[1] = v3[1] + ((float) 1 / 3) * (v2[1] - v3[1]);
			
			b3[0] = v2[0] - ((float) 1 / 3) * (v2[0] - v3[0]);
			b3[1] = v2[1] - ((float) 1 / 3) * (v2[1] - v3[1]);
			
			// Left Triangle
			
			float[] l1 = new float[2]; // Top Point
			float[] l2 = new float[2]; // Left Point
			float[] l3 = new float[2]; // Right Point
			
			l1[0] = ((float) 1 / 2) * (v2[0] + v1[0]) + ((float) Math.sqrt(3) / 6) * (v2[1] - v1[1]);
			l1[1] = ((float) 1 / 2) * (v2[1] + v1[1]) + ((float) Math.sqrt(3) / 6) * (v1[0] - v2[0]);
			
			l2[0] = v2[0] + ((float) 1 / 3) * (v1[0] - v2[0]);
			l2[1] = v2[1] + ((float) 1 / 3) * (v1[1] - v2[1]);
			
			l3[0] = v1[0] - ((float) 1 / 3) * (v1[0] - v2[0]);
			l3[1] = v1[1] - ((float) 1 / 3) * (v1[1] - v2[1]);
			
			// Right Triangle
			
			float[] r1 = new float[2]; // Top Point
			float[] r2 = new float[2]; // Left Point
			float[] r3 = new float[2]; // Right Point
			
			r1[0] = ((float) 1 / 2) * (v1[0] + v3[0]) + ((float) Math.sqrt(3) / 6) * (v1[1] - v3[1]);
			r1[1] = ((float) 1 / 2) * (v1[1] + v3[1]) + ((float) Math.sqrt(3) / 6) * (v3[0] - v1[0]);
			
			r2[0] = v1[0] + ((float) 1 / 3) * (v3[0] - v1[0]);
			r2[1] = v1[1] + ((float) 1 / 3) * (v3[1] - v1[1]);
			
			r3[0] = v3[0] - ((float) 1 / 3) * (v3[0] - v1[0]);
			r3[1] = v3[1] - ((float) 1 / 3) * (v3[1] - v1[1]);
			
			// Recurse
			processLine(b1, b2, b3, n - 1);
			processLine(l1, l2, l3, n - 1);
			processLine(r1, r2, r3, n - 1);*/
		}
		else
		{
			drawLine(v1, v2); // Draw
		}
	}
	
	private void drawLine(float[] v1, float[] v2)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		m_vertexPositions[0] = v1[0];
		m_vertexPositions[1] = v1[1];
		m_vertexPositions[2] = v2[0];
		m_vertexPositions[3] = v2[1];
		
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(m_vertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		
		// Draw now.
		gl.glDrawArrays(GL_LINES, 0, 2);
	}
	
	/*private void drawTriangle(float[] v1, float[] v2, float[] v3)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		// Store points in backing store
		m_vertexPositions[0] = v1[0];
		m_vertexPositions[1] = v1[1];
		m_vertexPositions[2] = v2[0];
		m_vertexPositions[3] = v2[1];
		m_vertexPositions[4] = v3[0];
		m_vertexPositions[5] = v3[1];
		
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(m_vertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		
		// Draw now
		gl.glDrawArrays(GL_TRIANGLES, 0, 3);
		
	}*/
	
}
