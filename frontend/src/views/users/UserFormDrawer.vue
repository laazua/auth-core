<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue';
import { ElForm, ElFormItem } from 'element-plus';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';
import BaseSelect from '@/components/BaseSelect.vue';
import type { UserVO, UserCreateRequest, UserUpdateRequest } from '@/types/user';

interface Props {
  visible: boolean;
  mode: 'create' | 'edit';
  initialData?: UserVO | null;
}

interface Emits {
  submit: [data: UserCreateRequest | UserUpdateRequest];
  close: [];
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  mode: 'create',
  initialData: null,
});

const emit = defineEmits<Emits>();

const formRef = ref<InstanceType<typeof ElForm>>();

const form = reactive<UserCreateRequest>({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  phone: '',
  status: 1,
  roleIds: [],
});

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为6-30位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 50, message: '昵称长度不能超过50位', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' },
  ],
};

function validateConfirmPassword(rule: any, value: string, callback: (error?: Error) => void) {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
}

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const isEditMode = computed(() => props.mode === 'edit');
const drawerTitle = computed(() => isEditMode.value ? '编辑用户' : '新增用户');

const resetForm = () => {
  form.username = '';
  form.password = '';
  form.confirmPassword = '';
  form.nickname = '';
  form.email = '';
  form.phone = '';
  form.status = 1;
  form.roleIds = [];
  formRef.value?.clearValidate();
};

const fillForm = (data: UserVO) => {
  form.username = data.username;
  form.nickname = data.nickname;
  form.email = data.email;
  form.phone = data.phone;
  form.status = data.status;
};

const handleSubmit = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      if (isEditMode.value) {
        const { password, confirmPassword, username, ...updateData } = form;
        emit('submit', updateData as UserUpdateRequest);
      } else {
        const { confirmPassword, ...createData } = form;
        emit('submit', createData as UserCreateRequest);
      }
    }
  });
};

const handleClose = () => {
  emit('close');
};

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      resetForm();
      if (isEditMode.value && props.initialData) {
        fillForm(props.initialData);
      }
      nextTick(() => {
        formRef.value?.clearValidate();
      });
    } else {
      resetForm();
    }
  },
  { immediate: true }
);

watch(
  () => props.initialData,
  (newVal) => {
    if (isEditMode.value && newVal && props.visible) {
      fillForm(newVal);
    }
  }
);

defineOptions({
  name: 'UserFormDrawer',
});
</script>

<template>
  <el-drawer
    v-model="props.visible"
    :title="drawerTitle"
    direction="rtl"
    size="480px"
    :before-close="handleClose"
    @close="emit('close')"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      class="user-form"
    >
      <el-form-item
        v-if="!isEditMode"
        label="用户名"
        prop="username"
      >
        <BaseInput
          v-model="form.username"
          placeholder="请输入用户名"
          maxlength="20"
        />
      </el-form-item>

      <el-form-item
        v-else
        label="用户名"
      >
        <BaseInput
          v-model="form.username"
          placeholder="用户名不可修改"
          readonly
          disabled
        />
      </el-form-item>

      <el-form-item
        label="密码"
        prop="password"
      >
        <BaseInput
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          show-password
          maxlength="30"
          :disabled="isEditMode"
        />
        <template #error>
          <span v-if="isEditMode">编辑时留空表示不修改密码</span>
        </template>
      </el-form-item>

      <el-form-item
        v-if="!isEditMode"
        label="确认密码"
        prop="confirmPassword"
      >
        <BaseInput
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          show-password
          maxlength="30"
        />
      </el-form-item>

      <el-form-item
        v-else
        label="确认密码"
        prop="confirmPassword"
      >
        <BaseInput
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          show-password
          maxlength="30"
          :disabled="isEditMode"
        />
        <template #error>
          <span v-if="isEditMode">编辑时留空表示不修改密码</span>
        </template>
      </el-form-item>

      <el-form-item label="昵称" prop="nickname">
        <BaseInput
          v-model="form.nickname"
          placeholder="请输入昵称"
          maxlength="50"
        />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <BaseInput
          v-model="form.email"
          type="email"
          placeholder="请输入邮箱"
          maxlength="100"
        />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <BaseInput
          v-model="form.phone"
          type="tel"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <BaseSelect
          v-model="form.status"
          :options="statusOptions"
          placeholder="请选择状态"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="user-form__footer">
        <BaseButton variant="default" @click="handleClose">取消</BaseButton>
        <BaseButton variant="primary" @click="handleSubmit" :loading="props.visible">确定</BaseButton>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.user-form {
  padding: 8px 4px 0;
}

.user-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  margin-top: 8px;
  border-top: 1px solid var(--color-border-light);
}

:deep(.el-drawer__body) {
  padding: 0 24px 24px;
}

:deep(.el-drawer__header) {
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border-light);
}

:deep(.el-drawer__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
</style>