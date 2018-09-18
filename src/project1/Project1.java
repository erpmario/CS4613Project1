package project1;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import graphicslib3D.GLSLUtils;
import graphicslib3D.Matrix3D;

import javax.swing.*;
import java.nio.FloatBuffer;
import java.util.Random;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static graphicslib3D.GLSLUtils.readShaderSource;

public class Project1 extends JFrame implements GLEventListener
{
	private GLCanvas myCanvas;
	private int rendering_program;
	private int vao[] = new int[1];
	private int vbo[] = new int[2];
	private float cameraX, cameraY, cameraZ;
	private GLSLUtils util = new GLSLUtils();
	private int npoints = 20000;
	
	public Project1()
	{
		setTitle("Project 1 - Koch Snowflake");
		setSize(600, 600);
		// Making sure we get a GL4 context for the canvas
		GLProfile profile = GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities = new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
		// end GL4 context
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		this.setVisible(true);
	}
	
	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(rendering_program);
		
		int mv_loc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");
		
		float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		Matrix3D pMat = orthogonal(-1.5f, 1.5f, 1.5f, -1.5f, 0.1f, 1000.0f);
		
		Matrix3D vMat = new Matrix3D();
		vMat.translate(-cameraX, -cameraY, -cameraZ);
		//Just drawing 2D - not moving the object
		Matrix3D mMat = new Matrix3D();
		mMat.setToIdentity();
		
		Matrix3D mvMat = new Matrix3D();
		mvMat.concatenate(vMat);
		mvMat.concatenate(mMat);
		
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvMat.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);
		
		gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0); // We are only passing two components
		gl.glEnableVertexAttribArray(0);
		
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		//gl.glPointSize(6.0f);
		
		gl.glDrawArrays(GL_POINTS, 0, npoints);
	}
	
	public void init(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) drawable.getGL();
		rendering_program = createShaderProgram();
		setupVertices();
		cameraX = 0.0f;
		cameraY = 0.0f;
		cameraZ = 3.0f;
	}
	
	private void setupVertices()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		Random random = new Random();
		int randv;
		float[] vertex_positions = new float[npoints * 2];
		float[] p1 = new float[2];
		float[] p0 = new float[2];
		// The first three vertices define the starting triangle
		// Equilateral triangle centered at the origin
		float side_length = 2.0f;
		// Top vertex - x and y
		vertex_positions[0] = 0;
		vertex_positions[1] = side_length * (float) Math.sqrt(3) / 3;
		// Bottom left
		vertex_positions[2] = -0.5f * side_length;
		vertex_positions[3] = -(float) Math.sqrt(3) * side_length / 6;
		// Bottom right
		vertex_positions[4] = 0.5f * side_length;
		vertex_positions[5] = -(float) Math.sqrt(3) * side_length / 6;
		// Pick a random vertex p0 inside the triangle (use affine sum)
		float alpha1, alpha2, alpha3;
		alpha1 = random.nextFloat();
		alpha2 = random.nextFloat() * (1 - alpha1);
		alpha3 = 1 - alpha1 - alpha2;
		p0[0] = alpha1 * vertex_positions[0] + alpha2 * vertex_positions[2] + alpha3 * vertex_positions[4];
		p0[1] = alpha1 * vertex_positions[1] + alpha2 * vertex_positions[3] + alpha3 * vertex_positions[5];
		
		for(int i = 3; i < npoints; i++)
		{
			// Select random vertex
			randv = random.nextInt(3);
			// Calculate middle point to random vertex
			for(int j = 0; j < 2; j++)
			{
				p1[j] = (p0[j] + vertex_positions[2 * randv + j]) / 2;
			}
			// Add p1 to the list of p oints to be drawn
			vertex_positions[2 * i] = p1[0];
			vertex_positions[2 * i + 1] = p1[1];
			// p0=p1
			p0[0] = p1[0];
			p0[1] = p1[1];
		}
		
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(vertex_positions);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
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
		new Sierpinski2D();
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
}
