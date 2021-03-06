/* @file
 *****************************************************************************
 * @author     This file is part of zkspark, developed by SCIPR Lab
 *             and contributors (see AUTHORS).
 * @copyright  MIT license (see LICENSE file)
 *****************************************************************************/

package relations.objects;

import algebra.fields.AbstractFieldElementExpanded;

import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * A system of R1CSRelation constraints looks like
 * <p>
 * { < A_k , X > * < B_k , X > = < C_k , X > }_{k=1}^{n}  .
 * <p>
 * In other words, the system is satisfied if and only if there exist a
 * USCS variable assignment for which each R1CSRelation constraint is satisfied.
 * <p>
 * NOTE:
 * The 0-th variable (i.e., "x_{0}") always represents the constant 1.
 * Thus, the 0-th variable is not included in num_variables.
 */
public class R1CSConstraints<FieldT extends AbstractFieldElementExpanded<FieldT>> implements
        Serializable {

    private ArrayList<R1CSConstraint<FieldT>> constraints;

    public R1CSConstraints() {
        constraints = new ArrayList<>();
    }

    public boolean add(final R1CSConstraint<FieldT> constraint) {
        return constraints.add(constraint);
    }

    public R1CSConstraint<FieldT> set(final int i, final R1CSConstraint<FieldT> constraint) {
        return constraints.set(i, constraint);
    }

    public R1CSConstraint<FieldT> get(final int i) {
        return constraints.get(i);
    }

    public ArrayList<R1CSConstraint<FieldT>> constraints() {
        return constraints;
    }

    public int size() {
        return constraints.size();
    }

    @Override
    public String toString() {
        return Arrays.toString(this.constraints.toArray());
    }
}
