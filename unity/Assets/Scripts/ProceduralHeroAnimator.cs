using UnityEngine;

namespace Wildland3D
{
    public sealed class ProceduralHeroAnimator : MonoBehaviour
    {
        public Transform leftArm, rightArm, leftLeg, rightLeg, head;
        public AudioSource footstep;
        private float move;
        private float t;
        private int lastStep;

        public void SetMoveAmount(float amount) => move = Mathf.Clamp01(amount);

        private void Update()
        {
            t += Time.deltaTime * Mathf.Lerp(1.2f, 9.5f, move);
            float swing = Mathf.Sin(t) * 34f * move;
            if (leftArm) leftArm.localRotation = Quaternion.Euler(swing, 0, 0);
            if (rightArm) rightArm.localRotation = Quaternion.Euler(-swing, 0, 0);
            if (leftLeg) leftLeg.localRotation = Quaternion.Euler(-swing * .8f, 0, 0);
            if (rightLeg) rightLeg.localRotation = Quaternion.Euler(swing * .8f, 0, 0);
            if (head) head.localPosition = new Vector3(head.localPosition.x, 1.72f + Mathf.Sin(t * .35f) * .015f, head.localPosition.z);
            int step = Mathf.FloorToInt(t / Mathf.PI);
            if (move > .2f && step != lastStep)
            {
                lastStep = step;
                if (footstep) footstep.Play();
            }
        }
    }
}
