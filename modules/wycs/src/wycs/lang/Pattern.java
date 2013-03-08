package wycs.lang;

import java.util.ArrayList;
import java.util.Collection;

import wybs.lang.SyntacticElement;
import wybs.lang.Attribute;

/**
 * Represents a type pattern which is used for pattern matching.
 * 
 * @author djp
 * 
 */
public abstract class Pattern extends SyntacticElement.Impl {
	
	/**
	 * The variable name associated with this type pattern. Maybe
	 * <code>null</code> if not declared variable.
	 */
	public String var;
	
	public Pattern(String var, Attribute... attributes) {
		super(attributes);
		this.var = var;
	}
	
	public Pattern(String var, Collection<Attribute> attributes) {
		super(attributes);
		this.var = var;
	}
	
	public abstract SyntacticType toSyntacticType();
	
	public static class Leaf extends Pattern {
		public SyntacticType type;
		
		public Leaf(SyntacticType type, String var, Attribute... attributes) {
			super(var,attributes);
			this.type = type;
		}
		
		public Leaf(SyntacticType type, String var, Collection<Attribute> attributes) {
			super(var,attributes);
			this.type = type;			
		}
		
		@Override
		public SyntacticType toSyntacticType() {
			return type;
		}
	}
	
	public static class Tuple extends Pattern {
		public Pattern[] patterns;
		
		public Tuple(Pattern[] patterns, String var, Attribute... attributes) {
			super(var,attributes);
			this.patterns = patterns;
		}

		public Tuple(Pattern[] patterns, String var, Collection<Attribute> attributes) {
			super(var,attributes);
			this.patterns = patterns;
		}
		
		@Override
		public SyntacticType.Tuple toSyntacticType() {
			SyntacticType[] types = new SyntacticType[patterns.length];
			for (int i = 0; i != types.length; ++i) {
				types[i] = patterns[i].toSyntacticType();
			}
			return new SyntacticType.Tuple(types);
		}
	}
}