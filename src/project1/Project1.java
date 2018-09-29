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
	private int m_n = 4; // Recursion level
	private float[] m_vertexPositions = new float[3 * 2]; // Three points, two coordinates
	
	public Project1()
	{
		setTitle("Project 1 - Koch Snowflake");
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
		float sideLength = 2.0f;
		// Top vertex - x and y
		v1[0] = 0;
		v1[1] = sideLength * (float) Math.sqrt(3) / 3;
		// Bottom left
		v2[0] = -0.5f * sideLength;
		v2[1] = -(float) Math.sqrt(3) * sideLength / 6;
		// Bottom right
		v3[0] = 0.5f * sideLength;
		v3[1] = -(float) Math.sqrt(3) * sideLength / 6;
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
		
		drawTriangle(v1, v2, v3);
		//processTriangle(v1, v2, v3, m_n);
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
		new Project1();
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
	
	/**
	 * Calculates the distance between two points in two-dimensional space. Arguments should be two-dimensional float arrays. Index 0 is assumed to be the x-coordinate, and index 1 is assumed to be
	 * the y-coordinate.
	 *
	 * @param v1
	 * 		The first point.
	 * @param v2
	 * 		The second point.
	 *
	 * @return A float representing the distance between the two points as calculated by the distance formula.
	 */
	private float distanceBetween(float[] v1, float[] v2)
	{
		return (float) Math.sqrt(Math.pow(v2[0] - v1[0], 2) + Math.pow(v2[1] - v1[1], 2));
	}
	
	// Processing triangles
	private void processTriangle(float[] v1, float[] v2, float[] v3, int n)
	{
		
		if(n > 0) // Recurse
		{
			// Coordinates for middle points
			float[] m1 = new float[2];
			float[] m2 = new float[2];
			float[] m3 = new float[2];
			for(int i = 0; i < 2; i++)
			{
				m1[i] = (v1[i] + v2[i]) / 2;
				m2[i] = (v2[i] + v3[i]) / 2;
				m3[i] = (v1[i] + v3[i]) / 2;
			}
			// Recurse
			processTriangle(m1, v2, m2, n - 1);
			processTriangle(v1, m1, m3, n - 1);
			processTriangle(m3, m2, v3, n - 1);
		}
		else
		{
			drawTriangle(v1, v2, v3); // Draw
		}
		
	}
	
	private void drawTriangle(float[] v1, float[] v2, float[] v3)
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
		
	}
	
}
